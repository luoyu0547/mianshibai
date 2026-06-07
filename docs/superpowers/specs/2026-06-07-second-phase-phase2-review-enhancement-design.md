# 面试吧二期 Phase 2 复盘增强设计

> 状态：设计稿，待用户审阅  
> 日期：2026-06-07  
> 范围：面试报告增强、异步优秀回答生成、报告对比、能力雷达、技能缺口分析

## 1. 背景与目标

Phase 1 已经补齐“简历导入 -> 职位分析 -> 简历优化 -> 岗位定向面试 -> 基础分析”的主链路。Phase 2 不继续扩展求职管理或简历版本功能，而是聚焦用户完成一次模拟面试之后如何复盘、对比和持续训练。

Phase 2 的产品目标是：让面试报告从“分数和建议”升级为“逐题复盘 + 标准答案参考 + 能力变化 + 技能缺口”的训练反馈系统。

核心体验：

```text
完成面试 -> 基础报告立即可见 -> 异步生成增强复盘 -> 查看逐题优秀回答 -> 对比历史报告 -> 获得能力雷达和技能缺口
```

## 2. 范围

### 2.1 包含能力

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 报告增强任务 | 报告生成后创建异步增强任务，状态可查询 | P0 |
| 逐题优秀回答 | 为每轮面试生成优秀回答、改进版回答、考察点和问题诊断 | P0 |
| 整体复盘摘要 | 汇总本次面试表现、主要短板和下一步训练建议 | P0 |
| 报告对比 | 对比两次面试报告的总分、维度分、缺口和建议变化 | P0 |
| 能力雷达 | 汇总技术准确性、表达清晰度、项目深度、岗位匹配和系统设计能力 | P0 |
| 技能缺口分析 | 从报告和岗位分析中汇总用户高频短板技能 | P0 |
| 失败重试 | 增强任务失败后支持用户手动重试 | P1 |

### 2.2 不包含能力

- 不做面试中断恢复。
- 不做简历 Diff 和版本回滚。
- 不做投递状态管理和求职漏斗。
- 不引入 RabbitMQ 或 Kafka。
- 不做面试录音回放。
- 不做跨用户排行榜或企业端看板。

## 3. 核心用户路径

1. 用户完成岗位定向或普通模拟面试。
2. 后端生成基础 `interview_report`，报告页立即可打开。
3. 系统创建报告增强任务，写入 DB 状态，并发布 Redis Stream 消息。
4. 前端报告页显示“复盘增强生成中”，轮询增强状态。
5. Worker 消费消息，调用 AI 生成逐题复盘和整体复盘，写入数据库。
6. 前端展示每轮问题的优秀回答、改进版回答、考察点和诊断。
7. 用户可选择与上一次报告或指定报告对比。
8. 数据分析页展示能力雷达、Top 技能缺口和近期变化。

## 4. 后端设计

### 4.1 数据模型

#### `interview_report_enhancement`

用于保存单份面试报告的增强复盘结果和任务状态。

字段建议：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户 ID |
| `session_id` | bigint | 面试会话 ID |
| `report_id` | bigint | 面试报告 ID |
| `status` | varchar(32) | `pending/running/completed/failed` |
| `summary` | text | 整体复盘摘要 |
| `radar_json` | json | 能力雷达数据 |
| `skill_gaps_json` | json | 技能缺口列表 |
| `action_items_json` | json | 下一步训练建议 |
| `error_message` | varchar(512) | 失败原因 |
| `retry_count` | int | 重试次数 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `is_delete` | tinyint | 逻辑删除 |

#### `interview_turn_review`

用于保存每轮回答的增强复盘结果。

字段建议：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户 ID |
| `session_id` | bigint | 面试会话 ID |
| `report_id` | bigint | 面试报告 ID |
| `turn_id` | bigint | 面试轮次 ID |
| `question` | text | 问题快照 |
| `answer_summary` | text | 用户回答摘要 |
| `diagnosis` | text | 回答问题诊断 |
| `excellent_answer` | text | 优秀回答示例 |
| `improved_answer` | text | 基于用户回答改写后的版本 |
| `knowledge_points_json` | json | 考察知识点 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `is_delete` | tinyint | 逻辑删除 |

### 4.2 异步任务机制

异步机制采用 Redis Stream + DB 状态落库。

推荐 Stream 名称：`interview.report.enhancement.stream`。

推荐 Consumer Group：`interview-report-enhancement-workers`。

消息体只包含最小信息：

```json
{
  "enhancementId": 1,
  "reportId": 10,
  "sessionId": 20,
  "userId": 100
}
```

处理流程：

1. `InterviewServiceImpl.generateReport` 保存基础报告后，创建 `interview_report_enhancement`，状态为 `pending`。
2. 发布 Redis Stream 消息。
3. Worker 获取消息后将状态改为 `running`。
4. Worker 查询报告、会话、轮次、职位和简历上下文。
5. Worker 调用 AI 生成结构化 JSON。
6. Worker 写入 `interview_turn_review` 和 `interview_report_enhancement`。
7. 成功后状态改为 `completed`，失败后状态改为 `failed` 并记录错误。

可靠性策略：

- DB 状态是最终事实来源，Redis Stream 只负责派发任务。
- Worker 处理前先检查状态，避免重复消费导致重复写入。
- 同一个 `report_id` 只允许一个有效增强记录。
- 失败后用户可通过接口触发重试，重试会递增 `retry_count`。
- Phase 2 不实现复杂死信队列，只保留失败状态和人工重试入口。

### 4.3 AI 输出结构

增强任务要求 AI 返回结构化 JSON，后端解析失败则标记任务失败。

推荐结构：

```json
{
  "summary": "本次面试整体表现...",
  "radar": {
    "accuracy": 78,
    "clarity": 72,
    "depth": 65,
    "matching": 70,
    "systemDesign": 55
  },
  "skillGaps": [
    {
      "name": "MySQL 索引优化",
      "severity": "high",
      "evidence": "回答中未说明联合索引最左前缀和回表成本"
    }
  ],
  "actionItems": [
    "复习 MySQL explain 和索引失效场景",
    "用 STAR 结构重写项目中性能优化经历"
  ],
  "turnReviews": [
    {
      "turnId": 1,
      "diagnosis": "回答覆盖了概念，但缺少项目场景和取舍分析",
      "excellentAnswer": "优秀回答示例...",
      "improvedAnswer": "基于用户回答改写后的版本...",
      "knowledgePoints": ["索引结构", "执行计划", "慢 SQL 排查"]
    }
  ]
}
```

### 4.4 接口设计

在现有 `/api/interview` 和 `/api/statistics` 下扩展，保持模块边界清晰。

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/interview/{sessionId}/report/enhancement` | GET | 获取报告增强状态和结果 |
| `/api/interview/{sessionId}/report/enhancement/retry` | POST | 失败后重试增强任务 |
| `/api/interview/reports/compare` | GET | 比较两份报告，参数为 `baseSessionId` 和 `targetSessionId` |
| `/api/statistics/analytics/review` | GET | 获取复盘分析总览：能力雷达、技能缺口、近期变化 |

### 4.5 服务边界

新增服务建议：

| 服务 | 职责 |
|------|------|
| `InterviewReportEnhancementService` | 创建任务、查询结果、重试任务、保存增强结果 |
| `InterviewReportEnhancementWorker` | Redis Stream 消费和 AI 调用编排 |
| `InterviewReportCompareService` | 两份报告对比 |
| `ReviewAnalyticsService` | 汇总能力雷达和技能缺口 |

`InterviewServiceImpl` 只负责在生成基础报告后触发增强任务，不承载增强 AI Prompt 和 Worker 逻辑。

## 5. 前端设计

### 5.1 报告页增强

`InterviewReportPage.vue` 增加增强复盘区域。

状态展示：

| 状态 | 展示 |
|------|------|
| `pending/running` | 显示“AI 正在生成逐题复盘”，定时轮询 |
| `completed` | 展示整体复盘、能力雷达、逐题优秀回答 |
| `failed` | 展示失败原因和“重新生成”按钮 |

逐题复盘卡片内容：

- 原问题。
- 我的回答摘要。
- 问题诊断。
- 优秀回答示例。
- 我的回答改进版。
- 考察知识点标签。

### 5.2 报告对比

报告对比可以先放在报告页内，提供“与上一次报告对比”入口。

展示内容：

- 总分变化。
- 维度分变化：accuracy、clarity、depth、matching、systemDesign。
- 新增/减少的技能缺口。
- 复盘建议变化。

Phase 2 不单独做复杂报告管理页，避免扩大范围。

### 5.3 分析页增强

扩展现有 `/analytics`，增加复盘分析区域。

展示内容：

- 能力雷达。
- Top 技能缺口。
- 最近 5 次面试总分趋势。
- 最近一次报告的下一步行动建议。

如果当前图表库不足，Phase 2 可先使用卡片和进度条表达雷达数据，不强制引入 ECharts。

## 6. 错误处理

| 场景 | 处理 |
|------|------|
| 报告不存在或不属于当前用户 | 返回 `INTERVIEW_NOT_FOUND_ERROR` 或 `NO_AUTH_ERROR` |
| 增强任务不存在 | 自动创建或返回空状态，由报告页触发重试 |
| AI 返回无法解析 | 标记任务 `failed`，记录错误摘要 |
| Redis 发布失败 | 保留 DB `pending` 状态，用户可重试；后台也可后续补偿 |
| 重复重试 | 如果状态是 `running`，拒绝重复触发 |
| 报告对比参数非法 | 返回 `PARAMS_ERROR` |

## 7. 测试策略

### 7.1 后端测试

- `InterviewReportEnhancementServiceTest`：任务创建、状态流转、重试规则。
- `InterviewReportEnhancementWorkerTest`：mock AI，验证成功写入和失败状态。
- `InterviewReportCompareServiceTest`：验证两份报告维度差异、技能缺口差异。
- `ReviewAnalyticsServiceTest`：验证能力雷达和技能缺口聚合。
- Controller 测试：验证权限、参数和响应结构。

### 7.2 前端测试

- 报告页增强状态渲染。
- 失败重试按钮调用正确 API。
- 对比结果为空或缺少历史报告时的空状态。
- 分析页在无增强报告时展示友好空状态。

### 7.3 验证命令

后端：

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package -DskipTests
```

前端：

```powershell
npm run type-check
npm run build-only
npm run test:unit
```

## 8. 成功标准

Phase 2 完成后，应满足：

- 基础报告生成后，增强复盘可异步生成且状态可见。
- 用户能在报告页看到逐题优秀回答和改进版回答。
- 用户能对比两次报告，看到分数和能力维度变化。
- 分析页能展示能力雷达和 Top 技能缺口。
- AI 增强失败不会影响基础报告查看。
- 不需要 RabbitMQ/Kafka，复用现有 Redis 基础设施。

## 9. 后续 Phase 3 候选

Phase 2 完成后，下一阶段可以继续补：

- 简历 Diff 和版本回滚。
- 投递状态管理和求职漏斗。
- 面试中断恢复。
- 批量导入职位。
- 学习建议任务化。

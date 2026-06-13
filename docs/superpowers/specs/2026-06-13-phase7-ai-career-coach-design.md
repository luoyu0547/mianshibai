# Phase 7: AI 求职教练设计

## 背景

Phase 1-6 已完成求职训练平台的主要功能闭环：简历、职位分析、模拟面试、AI 复盘、投递管理、八股训练、错题本、知识点掌握度、求职作战台和基础管理员后台。当前系统已经沉淀了足够多的用户求职数据，但这些数据仍分散在不同模块中。

Phase 7 的目标是新增一个“AI 求职教练”模块，把现有数据聚合成一次性诊断报告和 7 天行动计划，让用户知道自己当前最该补什么、今天应该做什么，以及历史诊断和计划如何变化。

## 目标

- 聚合用户现有求职数据，生成可保存的 AI 求职诊断报告。
- 基于诊断报告生成可保存的 7 天求职行动计划。
- 支持每日教练任务打卡、重开和计划进度统计。
- 支持用户查看历史诊断报告和历史教练计划。
- 使用“规则聚合 + AI 生成 + 规则兜底”的方式保证功能可用性。
- 在前端新增求职教练入口、首页、诊断详情和计划详情。

## 非目标

- 不做通知提醒、邮件提醒或定时推送。
- 不做管理员端教练内容管理。
- 不做复杂趋势图、日历视图或 BI 分析。
- 不做付费额度、AI 使用次数限制或商业化能力。
- 不把教练任务同步写入 `application_todo`。
- 不改变现有 `training_plan` 的八股训练语义。
- 不做在线算法判题。

## 产品范围

### 求职教练首页

用户访问 `/coach` 后看到：

- 最近一次诊断摘要。
- 当前活跃的 7 天教练计划。
- 今日任务列表。
- 计划完成进度。
- 生成新诊断与计划按钮。
- 历史诊断和历史计划入口。

当用户没有诊断报告时，页面展示空状态，引导用户生成第一份诊断与计划。

### 诊断报告

每次生成都会保存一份历史诊断。诊断报告包括：

- 总分，0-100。
- 诊断标题。
- 综合摘要。
- 优势列表。
- 短板列表。
- 优先行动建议。
- 数据完整度提示。
- 生成来源：`ai` 或 `fallback`。
- 生成时的数据快照。

数据快照用于记录当时聚合到的关键数据摘要，不用于还原完整业务对象。

### 7 天教练计划

每次生成都会保存一份历史计划，并与本次诊断关联。新计划生成后，旧的 `active` 计划自动归档为 `archived`，但历史仍可查看。

计划包括：

- 标题。
- 摘要。
- 目标岗位。
- 状态：`active` / `completed` / `archived`。
- 目标天数，固定为 7。
- 总任务数、已完成任务数。
- 每日任务分组。

计划状态规则：

- 新生成计划状态为 `active`。
- 同一用户只允许一个 `active` 教练计划。
- 用户完成所有任务后，计划自动标记为 `completed`。
- 生成新计划时，旧的 `active` 计划标记为 `archived`。

### 教练任务

教练任务独立于投递待办和八股训练计划。任务可以引用现有模块，但不修改被引用模块本身。

任务字段包括：

- Day 序号，1-7。
- 标题。
- 描述。
- 任务类型。
- 优先级。
- 状态。
- 可选引用类型和引用 ID。

任务类型：

- `resume`: 简历优化。
- `interview`: 面试复盘或模拟面试。
- `training`: 八股训练。
- `application`: 投递跟进。
- `job`: 职位分析。
- `habit`: 通用求职习惯或表达训练。

任务状态：

- `pending`: 未完成。
- `completed`: 已完成。

任务引用类型：

- `resume`
- `interview_session`
- `interview_report`
- `training_question`
- `training_plan`
- `job_application`
- `job`

## 后端设计

### 模块结构

新增标准分层：

- `CoachController`: `/api/coach` 前缀。
- `CoachService`: 求职教练业务接口。
- `CoachServiceImpl`: 数据聚合、AI 调用、兜底生成、任务状态更新。
- Mapper：`CoachDiagnosisMapper`、`CoachPlanMapper`、`CoachTaskMapper`。
- Entity：`CoachDiagnosis`、`CoachPlan`、`CoachTask`。
- DTO：生成请求、列表查询请求。
- VO：教练总览、诊断报告、计划详情、任务详情。

### 数据表

#### coach_diagnosis

保存每次求职诊断报告。

字段：

- `id`: 诊断 ID。
- `user_id`: 用户 ID。
- `title`: 诊断标题。
- `overall_score`: 综合评分。
- `summary`: 综合摘要。
- `strengths_json`: 优势列表。
- `weaknesses_json`: 短板列表。
- `suggestions_json`: 建议列表。
- `data_snapshot_json`: 数据快照。
- `data_completeness`: 数据完整度，0-100。
- `source`: `ai` / `fallback`。
- `create_time`, `update_time`, `is_delete`。

索引：

- `idx_user_id (user_id)`。
- `idx_create_time (create_time)`。

#### coach_plan

保存每次 7 天教练计划。

字段：

- `id`: 计划 ID。
- `user_id`: 用户 ID。
- `diagnosis_id`: 关联诊断 ID。
- `title`: 计划标题。
- `summary`: 计划摘要。
- `target_position`: 目标岗位。
- `target_days`: 固定为 7。
- `status`: `active` / `completed` / `archived`。
- `source`: `ai` / `fallback`。
- `create_time`, `update_time`, `is_delete`。

索引：

- `idx_user_id (user_id)`。
- `idx_diagnosis_id (diagnosis_id)`。
- `idx_status (status)`。

#### coach_task

保存教练计划下的每日任务。

字段：

- `id`: 任务 ID。
- `user_id`: 用户 ID。
- `plan_id`: 教练计划 ID。
- `day_index`: 1-7。
- `title`: 任务标题。
- `description`: 任务描述。
- `task_type`: 任务类型。
- `priority`: `high` / `medium` / `low`。
- `status`: `pending` / `completed`。
- `reference_type`: 可选引用类型。
- `reference_id`: 可选引用 ID。
- `completed_at`: 完成时间。
- `create_time`, `update_time`, `is_delete`。

索引：

- `idx_user_id (user_id)`。
- `idx_plan_id (plan_id)`。
- `idx_status (status)`。

### API 设计

#### 生成诊断与计划

`POST /api/coach/generate`

请求体：

```json
{
  "targetPosition": "Java 后端开发",
  "focus": "准备春招后端岗位"
}
```

字段说明：

- `targetPosition`: 可选。为空时优先使用用户资料中的目标岗位。
- `focus`: 可选。用户本次希望教练重点关注的问题。

处理规则：

1. 解析当前用户。
2. 聚合用户求职数据。
3. 构建结构化上下文。
4. 调用 AI 生成诊断和计划。
5. AI 响应解析失败或调用失败时，使用规则兜底生成基础诊断和任务。
6. 归档旧的活跃教练计划。
7. 保存新诊断、新计划和任务。
8. 返回新计划和诊断摘要。

#### 教练总览

`GET /api/coach/overview`

返回：

- 最近诊断。
- 当前活跃计划。
- 今日任务。
- 历史诊断数量。
- 历史计划数量。

#### 诊断历史

`GET /api/coach/diagnoses`

返回当前用户诊断报告列表，按创建时间倒序。

#### 诊断详情

`GET /api/coach/diagnoses/{id}`

只能访问当前用户自己的诊断报告。

#### 计划历史

`GET /api/coach/plans`

返回当前用户教练计划列表，按创建时间倒序。

#### 计划详情

`GET /api/coach/plans/{id}`

返回计划详情和任务列表。

#### 完成任务

`PUT /api/coach/tasks/{id}/complete`

规则：

- 只能操作当前用户自己的任务。
- 设置任务状态为 `completed`。
- 写入 `completed_at`。
- 如果计划下所有任务都已完成，计划状态更新为 `completed`。

#### 重开任务

`PUT /api/coach/tasks/{id}/reopen`

规则：

- 只能操作当前用户自己的任务。
- 设置任务状态为 `pending`。
- 清空 `completed_at`。
- 如果所属计划原状态为 `completed`，恢复为 `active`。

### 数据聚合来源

后端聚合以下数据：

- `user`: 目标岗位、技术方向、工作年限、城市、求职状态。
- `resume`: 最近简历数量和最近更新的简历。
- `job_application`: 投递状态分布和最近投递记录。
- `application_todo`: 未完成投递待办数量。
- `interview_session`: 最近面试状态和完成数量。
- `interview_report`: 最近面试评分。
- `interview_report_enhancement`: 最近技能缺口和行动建议。
- `training_plan`: 最近八股训练计划状态。
- `training_answer_review`: 最近批改结果。
- `training_mastery`: 弱项知识点和掌握度概览。
- `job`: 最近收藏或分析过的职位。

### AI 生成格式

AI 必须只返回 JSON：

```json
{
  "diagnosis": {
    "title": "Java 后端求职诊断",
    "overallScore": 72,
    "summary": "整体具备基础项目经验，但八股深度和面试表达需要加强。",
    "strengths": ["有 Spring Boot 项目基础"],
    "weaknesses": ["Redis 和 JVM 掌握度偏弱"],
    "suggestions": ["优先补齐 Redis 高频题和项目亮点表达"]
  },
  "plan": {
    "title": "7 天 Java 后端冲刺计划",
    "summary": "围绕简历、八股、面试表达和投递跟进安排每日任务。",
    "tasks": [
      {
        "dayIndex": 1,
        "title": "优化项目经历中的 Redis 表达",
        "description": "补充缓存穿透、缓存击穿和一致性处理说明。",
        "taskType": "resume",
        "priority": "high",
        "referenceType": "resume",
        "referenceId": 1
      }
    ]
  }
}
```

后端必须校验和裁剪 AI 响应：

- `overallScore` 限制在 0-100。
- `targetDays` 固定为 7，不使用 AI 返回天数。
- `dayIndex` 限制在 1-7。
- `taskType`、`priority`、`referenceType` 只允许枚举值。
- 任务总数控制在 14-21 个之间；不足时用规则补齐，过多时截断。

### 规则兜底

AI 不可用时仍生成可用结果：

- 数据完整度低时，建议先完善个人资料、简历和目标岗位。
- 有弱项掌握度时，优先生成八股训练任务。
- 有未完成投递待办时，生成投递跟进任务。
- 有已完成面试报告时，生成面试复盘任务。
- 没有足够数据时，生成通用 7 天求职启动计划。

兜底结果的 `source` 保存为 `fallback`。

### 权限与校验

- 所有接口需要登录。
- 用户只能访问自己的诊断、计划和任务。
- 任务完成和重开必须校验 `user_id`。
- 请求体长度需要限制，避免超长 `focus` 输入。
- AI 响应解析失败不向前端暴露原始响应。

## 前端设计

### 路由

新增用户侧路由：

- `/coach`: 求职教练首页。
- `/coach/diagnosis/:id`: 诊断详情。
- `/coach/plan/:id`: 计划详情。

路由 meta：

- `requiresAuth: true`

### 导航入口

在 `MainLayout.vue` 用户主导航新增“求职教练”入口。

### 数据层

新增：

- `src/types/coach.ts`
- `src/api/coach.ts`
- `src/stores/coach.ts`

Store 状态：

- `overview`
- `diagnoses`
- `plans`
- `currentDiagnosis`
- `currentPlan`
- `loading`
- `generating`

### 页面设计

#### CoachHomePage

展示：

- 页面标题和生成按钮。
- 最近诊断摘要卡片。
- 当前计划进度卡片。
- 今日任务卡片。
- 历史诊断列表。
- 历史计划列表。

生成按钮打开弹窗，填写目标岗位和本次重点。提交后调用 `POST /api/coach/generate`。

#### CoachDiagnosisDetailPage

展示：

- 诊断标题、分数、生成时间和来源。
- 综合摘要。
- 优势列表。
- 短板列表。
- 建议列表。
- 数据完整度提示。

#### CoachPlanDetailPage

展示：

- 计划标题、状态、进度。
- 按 Day 1-7 分组的任务列表。
- 每个任务支持完成和重开。
- 如果任务有引用类型，展示“去处理”入口，跳转到对应模块详情页。

### 视觉风格

沿用现有 Neubrutalism 设计系统：

- 暖白背景。
- 黑色 2px 边框。
- 紫色主按钮。
- 卡片偏移阴影。
- 使用现有 `NbButton` 和 `NbCard`。

## 错误处理

- 未登录：沿用现有登录态处理。
- 诊断、计划、任务不存在：返回 `NOT_FOUND_ERROR`。
- 访问他人数据：返回 `NO_AUTH_ERROR` 或 `NOT_FOUND_ERROR`，实现时保持当前项目风格一致。
- AI 调用失败：不直接失败，生成兜底结果并返回。
- 数据库保存失败：返回 `SYSTEM_ERROR`。

## 测试策略

### 后端

覆盖：

- 生成诊断与计划成功路径。
- AI 响应解析失败时走兜底。
- 生成新计划时归档旧 active 计划。
- 用户只能访问自己的诊断和计划。
- 任务完成后更新任务状态。
- 所有任务完成后计划变为 completed。
- 重开任务后 completed 计划恢复 active。
- Controller 端点路径和响应结构。

### 前端

覆盖：

- TypeScript 类型检查。
- Store 调用 API 后正确更新状态。
- 首页空状态、加载态和有数据状态。
- 任务完成/重开按钮行为。

## 成功标准

- 用户可以生成一份求职诊断和 7 天计划。
- 诊断、计划、任务都持久化保存，并可回看历史。
- 新计划生成后旧 active 计划被归档。
- AI 失败时用户仍能得到兜底诊断和计划。
- 用户可以完成和重开教练任务。
- 前端新增页面能完整展示教练闭环。
- 后端相关测试通过，前端 type-check 和 build 通过。

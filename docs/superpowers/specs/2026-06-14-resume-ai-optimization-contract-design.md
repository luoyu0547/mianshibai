# 简历 AI 优化字段契约设计

## 背景

当前简历 AI 优化存在前后端契约不一致的问题：后端提示词要求不输出 `highlights`，但单模块优化弹窗仍把 `highlights` 显示为“亮点标签”；后端统一强调 `description`，但教育经历实际使用 `activities` 字段，导致优化后难以正确覆盖“在校经历”。项目和工作描述也容易被 AI 写成大段文字，不适合当前简历模板展示。

本次优化目标是统一后端提示词、Java VO、前端 TypeScript 类型和前端展示字段，让 AI 只返回前端实际会消费和保存的内容。

## 范围

本次包含：

- 优化整份简历 AI 提示词和返回结构。
- 优化单模块 AI 提示词和前端字段展示。
- 优化 AI 评分提示词，使评分建议更具体、可执行。
- 移除或避免展示前端编辑器不存在、后端不应输出的字段。

本次不包含：

- 重做简历模板。
- 引入字段级 diff。
- 改造保存流程。
- 扩展新的简历模块类型。

## 统一字段契约

AI 只能返回当前前端编辑器和模板实际使用的字段。

### basic

允许字段：

- `name`
- `email`
- `phone`
- `targetPosition`
- `city`
- `github`
- `blog`
- `avatar`
- `currentStatus`
- `expectedLocation`
- `expectedSalary`
- `wechat`
- `website`

要求：保留联系方式和头像，不虚构个人信息。

### education

允许字段：

- `school`
- `major`
- `degree`
- `startDate`
- `endDate`
- `gpa`
- `activities`

要求：教育经历只优化 `activities` 作为“在校经历”，内容可包含课程、竞赛、奖项、社团、论文、校园项目等。不得返回 `description` 或 `highlights`。

### work

允许字段：

- `company`
- `position`
- `startDate`
- `endDate`
- `description`

要求：`description` 输出 2-4 条分点，每条包含“动作 + 技术/方法 + 结果”。不得返回 `highlights`。

### project

允许字段：

- `name`
- `role`
- `techStack`
- `startDate`
- `endDate`
- `description`

要求：`description` 输出 3-5 条分点，建议覆盖项目背景/职责、核心实现、技术难点、性能或业务结果。不得写成一大段过程描述，不得返回 `highlights`。

### skills

允许字段：

- `categories`

`categories` 项结构：

- `name`
- `items`

要求：只输出技能分类和技能项，不写自然段。

### summary

允许字段：

- `content`

要求：输出短自我介绍，突出定位、核心技能、项目经验和求职匹配度，不套用项目描述结构。

## 单模块优化设计

后端 `OPTIMIZE_SYSTEM_PROMPT` 需要按 `sectionType` 明确允许字段和写作规则。AI 返回结构必须与输入形态一致：输入为数组时返回数组，输入为对象时返回对象。

前端 `AiOptimizeDialog` 只展示实际存在和允许的字段：

- 教育经历显示“在校经历”对应 `activities`。
- 工作和项目显示“描述”对应 `description`。
- 技能显示“技能分类”对应 `categories`。
- 自我评价显示“内容”对应 `content`。
- 不再把 `highlights` 作为默认展示字段。

应用逻辑保持不变：单模块优化结果仍通过现有 `handleOptimizeApplied` 合并到对应模块。

## 整份优化设计

后端 `ResumeWholeOptimizeVO` 增加 `sectionSummaries`，用于前端展示模块优化摘要。保存和回填仍只使用 `optimizedSections`。

返回结构：

```json
{
  "beforeScore": 70,
  "estimatedAfterScore": 82,
  "globalSuggestions": ["补充项目结果数据，强化岗位关键词。"],
  "sectionSummaries": [
    {
      "sectionType": "project",
      "sectionTitle": "项目经历",
      "summary": "将项目描述拆成分点，补充核心实现和量化结果。"
    }
  ],
  "optimizedSections": [
    {
      "sectionType": "project",
      "sectionData": {
        "name": "面试模拟平台",
        "role": "核心开发",
        "techStack": ["Java", "Spring Boot", "Vue 3"],
        "startDate": "2026-01",
        "endDate": "2026-05",
        "description": "1. 负责面试会话状态机设计，支撑 created 到 completed 的完整流程。\n2. 基于 Spring Boot 和 WebSocket 实现实时问答链路，降低用户等待时间。\n3. 接入 AI 评分和报告生成能力，提升面试复盘效率。"
      }
    }
  ]
}
```

`sectionSummaries` 字段：

- `sectionType`：模块类型。
- `sectionTitle`：前端展示标题。
- `summary`：一句话说明该模块优化了什么。

前端 `WholeResumeOptimizeDialog` 展示：

- 优化前评分。
- 预估优化后评分。
- 全局优化建议。
- 模块优化摘要列表。

前端不展示 JSON，不展示无用字段，不让用户处理 `optimizedSections` 的原始结构。

## 评分设计

评分接口返回结构不扩展，仍只包含前端实际消费字段：

- `score`
- `dimensions.completeness`
- `dimensions.completenessComment`
- `dimensions.professionalism`
- `dimensions.professionalismComment`
- `dimensions.matching`
- `dimensions.matchingComment`
- `suggestions`

后端评分提示词需要按模块字段评分：

- `education.activities`：看在校经历是否具体、有含金量。
- `work.description`：看是否分点、是否体现职责和成果。
- `project.description`：看是否包含核心实现、技术难点和结果。
- `skills.categories`：看技能分类是否清晰、是否匹配目标岗位。
- `summary.content`：看个人定位是否清楚。

`suggestions` 必须具体到模块和字段，例如：

- `项目经历 description 建议拆成 3 条：核心实现、技术难点、量化结果。`
- `教育经历 activities 建议补充竞赛、课程设计或校园项目成果。`

## 数据流

单模块优化：

1. 前端传入 `sectionType` 和当前 `sectionData`。
2. 后端按 `sectionType` 拼接字段规则提示词。
3. AI 返回与输入同形态的数据。
4. 前端弹窗按字段标签展示优化前后内容。
5. 用户应用后回填编辑页，保存流程不变。

整份优化：

1. 前端传入 `resumeId` 和可选岗位上下文。
2. 后端读取当前模块并裁剪长文本。
3. 后端评分得到 `beforeScore`。
4. AI 返回 `globalSuggestions`、`sectionSummaries`、`optimizedSections`。
5. 前端展示分数、全局建议和模块摘要。
6. 用户应用后按现有逻辑回填编辑页，保存流程不变。

## 错误处理

- AI 返回 JSON 解析失败时沿用现有异常处理。
- `sectionSummaries` 缺失时前端只展示评分和全局建议。
- `optimizedSections` 缺失或为空时禁用应用按钮，避免覆盖为空内容。
- 后端提示词要求不得输出未允许字段，但前端仍只展示白名单字段，避免无效字段污染 UI。

## 验证

后端验证：

- 运行 Maven 测试或至少编译。
- 检查新增 VO 字段序列化正常。
- 检查提示词返回结构可被现有 JSON 提取逻辑解析。

前端验证：

- 运行 `npm run type-check`。
- 检查 `AiOptimizeDialog` 不再默认显示“亮点标签”。
- 检查 `WholeResumeOptimizeDialog` 能展示 `sectionSummaries`。
- 检查教育经历优化结果能覆盖 `activities`。

## 自审结论

- 无 TBD 或 TODO。
- 后端提示词、VO、TypeScript 类型和前端展示字段保持一致。
- 范围聚焦在 AI 输出契约、单模块展示、整份优化展示和评分提示词。
- 未引入字段级 diff 或保存流程改造，避免超出本次目标。

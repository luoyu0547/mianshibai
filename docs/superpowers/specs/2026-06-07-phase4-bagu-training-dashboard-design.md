# Phase 4: AI 八股训练闭环与求职作战台设计

## 背景

Phase 1 已完成简历、职位、岗位定向面试和基础分析闭环。Phase 2 已完成面试复盘增强，能够沉淀逐题诊断、优秀回答、能力雷达、技能缺口和行动建议。Phase 3 已补齐投递管理、待办中心和报告图表化展示。

当前系统已经可以发现用户在面试中的短板，但还缺少把短板转化为日常训练的问题闭环。对程序员求职者来说，训练重点主要是八股文和算法。算法训练适合在 LeetCode、力扣、CodeTop 等带 OJ 判题的平台完成；本系统不应重复建设 OJ。系统应该把重点放在八股文训练：根据用户短板生成题目，记录用户答案，使用 AI 批改，并沉淀掌握度与错因。

## 目标

- 将首页升级为求职作战台，让用户打开系统后知道今天该跟进什么、该练什么。
- 基于面试复盘、技能缺口、职位方向和历史训练记录生成个性化八股训练计划。
- 支持八股题文本作答，并保存用户答案。
- 支持 AI 对八股答案进行结构化批改，输出评分、遗漏点、错误点、参考答案和表达建议。
- 沉淀用户在不同知识点上的掌握状态，用于后续训练计划和作战台展示。
- 为算法训练提供外部 OJ 刷题建议，但不做算法代码批改或在线判题。

## 非目标

- 不做在线判题系统。
- 不运行用户提交的算法代码。
- 不批改算法代码正确性。
- 不接入 LeetCode、力扣、CodeTop 等第三方账号。
- 不建设公共题库市场或题库管理后台。
- 不做邮件、短信、日历同步或桌面通知。
- 不做复杂学习路径系统或课程系统。

## 产品闭环

Phase 4 的核心闭环是：

1. 用户完成模拟面试或查看增强复盘。
2. 系统从能力雷达、技能缺口、逐题诊断和岗位方向中识别薄弱知识点。
3. AI 生成八股训练计划，并附带算法刷题建议。
4. 用户按计划完成八股题文本作答。
5. AI 批改答案，输出分析记录。
6. 系统更新题目状态、知识点掌握度和作战台今日行动。

## 功能设计

### 求职作战台

首页 `/` 从普通欢迎页升级为求职作战台，聚合已有数据和 Phase 4 新增训练数据。

展示内容：

- 今日优先事项：未完成高优先级投递待办、今日到期待办、待完成八股训练题。
- 投递进展摘要：投递总数、面试中数量、Offer 数、失败或放弃数量。
- 当前八股训练计划：计划名称、进度、今日推荐题目。
- 最近薄弱知识点：来自增强复盘和训练批改记录。
- 算法刷题建议：推荐题型、推荐平台、题目编号或题名。
- 快捷入口：开始面试、生成训练计划、进入八股训练、查看投递待办。

作战台不承担复杂编辑功能，只作为“今天做什么”的入口。

### 八股训练计划

训练计划由 AI 生成，也允许用户重新生成。

生成依据：

- 最近 20 次增强复盘中的 `radar`、`skillGaps`、`actionItems`。
- 最近训练批改记录中的高频遗漏点和低掌握度知识点。
- 用户最近关注或投递的职位方向。
- 用户当前简历中的技术栈和项目经历。

计划内容：

- 计划标题，例如“Java 后端面试八股强化计划”。
- 计划周期，例如 3 天或 7 天。
- 目标知识点，例如 JVM、MySQL、Redis、Spring、计算机网络、操作系统、分布式、项目经历。
- 每日八股题列表。
- 算法刷题建议列表。

计划状态：

- `active`: 当前执行中。
- `completed`: 已完成。
- `archived`: 已归档。

同一用户同一时间只保留一个推荐的 `active` 计划。重新生成计划时，旧计划归档，新计划激活。

### 八股题训练

八股题是 Phase 4 的核心训练对象。

题目字段：

- 题目标题。
- 题目正文。
- 主题分类，例如 Java、Spring、MySQL、Redis、JVM、网络、操作系统、分布式、项目。
- 技能标签，例如 `索引`、`事务`、`AOP`、`缓存穿透`。
- 难度：`easy` / `medium` / `hard`。
- 来源：增强复盘、技能缺口、职位方向、手动生成。
- 参考答案要点。
- 追问建议。

题目状态：

- `pending`: 未作答。
- `answered`: 已作答，待查看或已生成批改。
- `reviewed`: 已批改。
- `mastered`: 用户标记已掌握。
- `skipped`: 用户跳过。

用户可以对每道题提交文本答案。每次提交都生成一条答案记录，保留历史，不覆盖旧答案。

### AI 批改记录

AI 批改只针对八股题文本答案。

批改输出：

- 总评分：0-100。
- 技术准确性评分。
- 表达清晰度评分。
- 深度评分。
- 项目结合度评分。
- 答案优点。
- 错误点。
- 遗漏点。
- 推荐回答。
- 表达优化建议。
- 后续追问题。
- 掌握度建议：`weak` / `basic` / `good` / `mastered`。

批改记录应保存为结构化 JSON 字段，方便后续统计和展示。

### 算法刷题建议

算法部分只做外部 OJ 刷题建议。

建议内容：

- 题型，例如数组、链表、哈希、双指针、滑动窗口、二分、栈、树、图、动态规划、回溯。
- 推荐平台，例如 LeetCode、力扣、CodeTop。
- 推荐题目编号或题名，例如 LeetCode 3、11、15、206、215。
- 推荐理由，例如“近期岗位偏后端开发，建议巩固高频数组和链表题”。
- 建议周期，例如“本周完成 5 道”。

系统不保存算法答案，不批改算法代码，只允许用户标记“已完成刷题建议”。

## 数据模型

### `training_plan`

表示一次八股训练计划。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `title`: 计划标题。
- `source_type`: 来源，`review` / `job` / `manual`。
- `source_id`: 来源 ID，可为空。
- `target_days`: 计划天数。
- `status`: `active` / `completed` / `archived`。
- `summary`: 计划说明。
- `focus_topics`: JSON 数组，重点知识点。
- `create_time` / `update_time` / `is_delete`: 通用字段。

### `training_question`

表示训练计划中的八股题。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `plan_id`: 训练计划 ID。
- `day_index`: 第几天训练。
- `title`: 题目标题。
- `content`: 题目正文。
- `topic`: 主题分类。
- `skill_tags`: JSON 数组。
- `difficulty`: `easy` / `medium` / `hard`。
- `source_type`: `review_gap` / `job_requirement` / `resume_project` / `manual`。
- `reference_answer`: 参考答案或答案要点。
- `follow_up_questions`: JSON 数组。
- `status`: `pending` / `answered` / `reviewed` / `mastered` / `skipped`。
- `create_time` / `update_time` / `is_delete`: 通用字段。

### `training_answer`

表示用户一次八股题作答。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `question_id`: 八股题 ID。
- `answer_text`: 用户答案。
- `create_time` / `update_time` / `is_delete`: 通用字段。

### `training_answer_review`

表示 AI 对一次八股答案的批改记录。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `question_id`: 八股题 ID。
- `answer_id`: 答案 ID。
- `total_score`: 总评分。
- `accuracy_score`: 技术准确性。
- `clarity_score`: 表达清晰度。
- `depth_score`: 深度。
- `project_score`: 项目结合度。
- `strengths_json`: 优点。
- `mistakes_json`: 错误点。
- `missing_points_json`: 遗漏点。
- `suggestions_json`: 表达和复习建议。
- `recommended_answer`: 推荐回答。
- `follow_up_questions_json`: 后续追问。
- `mastery_level`: `weak` / `basic` / `good` / `mastered`。
- `create_time` / `update_time` / `is_delete`: 通用字段。

### `algorithm_recommendation`

表示算法外部刷题建议。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `plan_id`: 训练计划 ID。
- `category`: 题型。
- `platform`: 推荐平台。
- `problem_ref`: 题目编号或题名。
- `reason`: 推荐理由。
- `completed`: 是否完成。
- `create_time` / `update_time` / `is_delete`: 通用字段。

## 后端模块

新增标准分层：

- `TrainingController`: `/api/training` 前缀。
- `TrainingService`: 训练计划、八股题、作答、批改和算法建议业务逻辑。
- `DashboardController`: `/api/dashboard` 前缀。
- `DashboardService`: 求职作战台聚合逻辑。
- Mapper：`TrainingPlanMapper`、`TrainingQuestionMapper`、`TrainingAnswerMapper`、`TrainingAnswerReviewMapper`、`AlgorithmRecommendationMapper`。
- Entity：对应 5 张新增表。
- DTO：生成计划请求、提交答案请求、题目状态更新请求、算法建议完成请求。
- VO：作战台总览、训练计划详情、题目详情、答案批改详情。

AI 调用通过现有 Spring AI `ChatClient`。生成计划和批改答案均需要明确 JSON 输出格式，并对解析失败返回 `AI_RESPONSE_PARSE_ERROR`。

## API 设计

### 作战台 API

- `GET /api/dashboard`: 获取作战台总览。

返回内容：

- 今日优先事项。
- 投递统计摘要。
- 当前训练计划摘要。
- 待训练八股题。
- 最近薄弱知识点。
- 算法刷题建议。

### 训练计划 API

- `POST /api/training/plan/generate`: AI 生成训练计划。
- `GET /api/training/plan/active`: 获取当前激活计划。
- `GET /api/training/plan`: 获取训练计划列表。
- `GET /api/training/plan/{id}`: 获取训练计划详情。
- `PUT /api/training/plan/{id}/archive`: 归档训练计划。
- `PUT /api/training/plan/{id}/complete`: 完成训练计划。

### 八股题 API

- `GET /api/training/question/{id}`: 获取题目详情。
- `PUT /api/training/question/{id}/master`: 标记已掌握。
- `PUT /api/training/question/{id}/skip`: 跳过题目。
- `POST /api/training/question/{id}/answer`: 提交文本答案并触发 AI 批改。
- `GET /api/training/question/{id}/answers`: 查看作答与批改历史。

### 算法建议 API

- `PUT /api/training/algorithm/{id}/complete`: 标记算法建议已完成。
- `PUT /api/training/algorithm/{id}/reopen`: 取消完成。

## 前端页面

### `/` 求职作战台

替换现有 `HomePage.vue` 的简单欢迎页。

模块：

- 顶部欢迎和今日状态。
- 今日优先事项卡片。
- 投递进展卡片。
- 当前八股训练计划卡片。
- 待练八股题列表。
- 最近薄弱知识点。
- 算法刷题建议。

### `/training`

训练中心页面。

模块：

- 当前激活计划。
- 计划生成按钮。
- 训练进度。
- 按天分组的八股题列表。
- 算法刷题建议列表。
- 历史计划入口。

### `/training/plan/:id`

训练计划详情页。

模块：

- 计划摘要。
- 每日训练题。
- 每题状态、得分和掌握度。
- 算法建议完成状态。

### `/training/question/:id`

八股题训练页。

模块：

- 题目正文。
- 标签、难度、来源。
- 用户答案输入框。
- 提交答案按钮。
- AI 批改结果。
- 推荐回答。
- 历史作答记录。
- 标记已掌握或跳过。

## 权限与校验

- 所有 API 从 JWT 解析当前用户 ID。
- 查询、更新、删除训练计划和题目时必须校验 `user_id`。
- 提交答案时必须校验题目归属当前用户。
- 批改记录必须绑定当前用户、题目和答案。
- 生成新计划时，如果已有 `active` 计划，应先将旧计划归档。
- 答案文本不能为空，长度需要限制，避免超长 prompt。

## AI Prompt 约束

生成训练计划时，AI 必须输出结构化 JSON：

- `title`
- `summary`
- `targetDays`
- `focusTopics`
- `questions`
- `algorithmRecommendations`

批改八股答案时，AI 必须输出结构化 JSON：

- `totalScore`
- `accuracyScore`
- `clarityScore`
- `depthScore`
- `projectScore`
- `strengths`
- `mistakes`
- `missingPoints`
- `suggestions`
- `recommendedAnswer`
- `followUpQuestions`
- `masteryLevel`

Prompt 需要强调：

- 八股批改应关注技术准确性、原理深度、表达结构和项目结合。
- 推荐回答应可用于面试口述，而不是只给百科式定义。
- 算法部分只生成刷题建议，不要求用户在系统内提交算法代码。

## 错误处理

- 参数错误返回 `PARAMS_ERROR`。
- 未登录返回 `NOT_LOGIN_ERROR`。
- 访问他人训练计划、题目、答案或批改记录返回 `NO_AUTH_ERROR`。
- 训练计划不存在返回新增错误码 `TRAINING_PLAN_NOT_FOUND_ERROR`。
- 训练题不存在返回新增错误码 `TRAINING_QUESTION_NOT_FOUND_ERROR`。
- 答案不存在返回新增错误码 `TRAINING_ANSWER_NOT_FOUND_ERROR`。
- AI 服务异常返回 `AI_SERVICE_ERROR`。
- AI JSON 解析失败返回 `AI_RESPONSE_PARSE_ERROR`。

## 测试策略

后端测试：

- 生成训练计划时归档旧 active 计划。
- 查询 active 计划只返回当前用户数据。
- 提交八股答案成功创建答案记录和批改记录。
- 查询他人题目被拒绝。
- 标记题目掌握成功更新状态。
- 标记算法建议完成成功。
- Dashboard 聚合今日待办、训练题和算法建议。
- AI 返回非法 JSON 时返回解析错误。
- SQL 初始化脚本包含 5 张新增表。

前端验证：

- `npm run type-check`。
- `npm run build-only`。
- 首页作战台空态、加载态和有数据状态。
- 训练中心生成计划、查看题目、提交答案、查看批改。

## 分期建议

Phase 4 可以拆为 8 个实现任务：

1. 后端训练表、错误码、Entity、Mapper。
2. 后端训练 DTO、VO、Service、Controller。
3. AI 生成计划与八股批改逻辑。
4. Dashboard 聚合 API。
5. 后端测试。
6. 前端训练类型、API、Store、路由。
7. 前端作战台与训练中心页面。
8. 前端题目作答与批改详情页面，全量验证。

## 成功标准

- 用户可以基于已有面试复盘生成一份八股训练计划。
- 用户可以完成八股题文本作答并看到 AI 批改记录。
- 系统能够保存每次作答和批改，供后续复盘查看。
- 首页作战台可以展示今日待办、训练计划、待练题目、薄弱知识点和算法建议。
- 算法训练只作为外部 OJ 刷题建议，不进入系统批改链路。

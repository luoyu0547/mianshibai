# Phase 5: 八股错题本与知识点掌握度设计

## 背景

Phase 4 已完成 AI 八股训练闭环：系统可以根据用户面试复盘、职位方向和简历信息生成八股训练计划；用户可以对八股题提交文本答案；AI 会输出结构化批改记录，包括评分、错误点、遗漏点、推荐答案和掌握度。

当前问题是：批改记录已经被保存，但用户还缺少系统化复习入口。低分题、薄弱知识点和反复出错的标签没有沉淀为错题本，也没有形成可视化掌握度。Phase 5 的目标是在不扩大到复杂学习系统的前提下，把 Phase 4 的批改数据转化为“复习什么”和“哪里薄弱”。

## 目标

- 提供八股错题本，集中展示低分、弱掌握或基础掌握的训练题。
- 提供知识点掌握度，按 topic 和 skillTags 聚合平均分、弱掌握次数、练习次数和最近练习时间。
- 在 Dashboard 中加入今日复习题、Top 薄弱知识点和掌握度摘要。
- 支持用户将错题标记为已掌握，并从错题本中弱化或移除。
- 保持算法训练边界不变：算法仍只做外部 OJ 刷题建议，不进入错题本和 AI 批改统计。

## 非目标

- 不做 AI 任务异步化。
- 不做复杂间隔复习算法。
- 不做日历提醒、站内信、邮件或短信提醒。
- 不做公共题库、收藏题库或题库管理后台。
- 不统计算法题正确率，也不接入 LeetCode、力扣、CodeTop 账号。
- 不重构 Phase 4 的训练计划生成和答案批改流程。

## 产品闭环

Phase 5 在 Phase 4 闭环后追加复习层：

1. 用户完成八股作答并获得 AI 批改。
2. 系统根据 `totalScore`、`masteryLevel`、错误点和遗漏点识别错题。
3. 系统按 topic 和 skillTags 更新知识点掌握度。
4. 用户在错题本中复习低掌握题目，查看历史作答、AI 错误点、遗漏点和推荐答案。
5. 用户可以将题目标记为已掌握。
6. Dashboard 根据错题本和掌握度推荐今日复习内容。

## 错题定义

一道题进入错题本，需要满足“未标记已掌握”，并且满足以下任一弱掌握条件：

- 最新批改 `masteryLevel` 为 `weak`。
- 最新批改 `masteryLevel` 为 `basic`。
- 最新批改 `totalScore` 小于 70。
题目状态为 `mastered` 后，默认不再作为待复习错题展示，但仍保留在历史记录中。

## 功能设计

### 错题本

新增页面 `/training/mistakes`。

展示内容：

- 错题列表。
- 题目标题、topic、skillTags、难度、最新得分、掌握度。
- 最新错误点、遗漏点和建议摘要。
- 最近作答时间。
- 入口：查看题目详情、查看历史作答、标记已掌握。

筛选能力：

- topic。
- masteryLevel：`weak` / `basic` / `good` / `mastered`。
- 是否已掌握。
- 分数区间：低于 60、60-69、70 以上。

排序规则：

- 默认优先显示 `weak`。
- 同掌握度下按最新作答时间倒序。
- 已掌握题默认隐藏。

### 知识点掌握度

新增页面 `/training/mastery`。

展示内容：

- topic 维度掌握度列表。
- skillTag 维度薄弱标签 Top 列表。
- 每个 topic 展示：练习题数、作答次数、平均分、弱掌握次数、掌握题数、最近练习时间。
- 掌握度等级：`weak` / `basic` / `good` / `mastered`。
- 点击 topic 可跳转到错题本并自动带上 topic 筛选。

掌握度计算建议：

- 平均分 < 60 或 weak 次数占比 >= 50%：`weak`。
- 平均分 60-74：`basic`。
- 平均分 75-89：`good`。
- 平均分 >= 90 且 weak 次数为 0：`mastered`。

### Dashboard 增强

增强现有 `/api/dashboard` 和首页作战台。

新增展示：

- 今日复习题：最多 5 道错题。
- Top 薄弱知识点：按 weak/basic 次数排序。
- 掌握度摘要：weak/basic/good/mastered 的 topic 数量。

Dashboard 仍然只作为入口和摘要，不承载完整错题管理。

## 数据模型

### 是否新增错题表

不新增独立错题表。错题本由以下已有表动态生成：

- `training_question`
- `training_answer`
- `training_answer_review`

理由：

- 错题是批改记录的派生视图，不是新的业务实体。
- 用户标记已掌握已经体现在 `training_question.status = mastered`。
- 避免错题表和题目状态之间产生一致性问题。

### 新增 `training_mastery`

新增聚合表 `training_mastery`，缓存 topic / skillTag 掌握度，避免每次页面加载都扫描所有批改记录。

字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `target_type`: `topic` / `skill_tag`。
- `target_name`: 知识点名称或技能标签。
- `practice_count`: 作答次数。
- `question_count`: 涉及题目数。
- `average_score`: 平均分。
- `weak_count`: weak 或 basic 次数。
- `mastered_count`: mastered 次数。
- `mastery_level`: `weak` / `basic` / `good` / `mastered`。
- `last_practiced_at`: 最近练习时间。
- `create_time` / `update_time` / `is_delete`: 通用字段。

更新时机：

- 每次八股答案批改成功后，基于该题的 topic 和 skillTags 更新对应 `training_mastery`。
- 用户将题目标记为已掌握后，重新计算该题对应 topic 和 skillTags。

## 后端设计

新增或扩展模块：

- `TrainingReviewController`: `/api/training/review` 前缀，提供错题本和掌握度接口。
- `TrainingReviewService`: 错题查询、掌握度查询、掌握度刷新。
- `TrainingMasteryMapper` / `TrainingMastery`: 新增聚合表 Mapper 和 Entity。
- 扩展 `TrainingServiceImpl`: 在批改成功和标记掌握后调用掌握度刷新逻辑。
- 扩展 `DashboardServiceImpl`: 增加今日复习题、薄弱知识点和掌握度摘要。

### API 设计

错题本：

- `GET /api/training/review/mistakes`: 查询错题本。

查询参数：

- `topic`: topic 过滤。
- `masteryLevel`: 掌握度过滤。
- `includeMastered`: 是否包含已掌握题，默认 false。
- `scoreMax`: 最高分过滤，可为空。

掌握度：

- `GET /api/training/review/mastery`: 查询 topic 掌握度列表。
- `GET /api/training/review/mastery/tags`: 查询 skillTag 掌握度列表。
- `POST /api/training/review/mastery/rebuild`: 重建当前用户掌握度统计，用于数据修复或首次上线后补齐历史数据。

Dashboard 扩展：

- `GET /api/dashboard`: 在现有返回值中增加 `reviewQuestions`、`weakMasteries`、`masterySummary`。

## 前端设计

### 路由

新增路由：

- `/training/mistakes`: 错题本。
- `/training/mastery`: 知识点掌握度。

### 训练中心入口

在 `/training` 页面增加两个入口卡片：

- 错题本。
- 知识点掌握度。

### 错题本页面

页面结构：

- 顶部统计：错题总数、weak 数、basic 数、已掌握数。
- 筛选区：topic、masteryLevel、是否包含已掌握、分数区间。
- 错题列表：每张卡显示题目、topic、标签、分数、掌握度、错误点、遗漏点、推荐答案摘要。
- 操作：查看题目详情、标记已掌握。

### 掌握度页面

页面结构：

- 掌握度摘要卡：weak/basic/good/mastered 数量。
- topic 掌握度列表：平均分、练习次数、弱掌握次数、最近练习时间。
- skillTag 薄弱标签列表。
- 点击 topic 跳转错题本筛选。

## 权限与校验

- 所有接口从 JWT 解析当前用户 ID。
- 错题本只查询当前用户的训练题和批改记录。
- 掌握度只查询和更新当前用户数据。
- 标记已掌握沿用现有题目归属校验。
- 重建掌握度只重建当前用户，不允许跨用户操作。

## 错误处理

- 参数错误返回 `PARAMS_ERROR`。
- 未登录返回 `NOT_LOGIN_ERROR`。
- 查询他人题目或掌握度数据返回 `NO_AUTH_ERROR` 或对应 not found 错误。
- 重建掌握度失败返回 `SYSTEM_ERROR`。

## 测试策略

后端测试：

- SQL 初始化包含 `training_mastery` 表。
- 批改答案后更新 topic 掌握度。
- 批改答案后更新 skillTag 掌握度。
- 错题本只返回 weak/basic 或低分题。
- 已掌握题默认不出现在错题本。
- 掌握度重建只处理当前用户数据。
- Dashboard 返回今日复习题和掌握度摘要。

前端验证：

- `npm run type-check`。
- `npm run build-only`。
- 错题本空态、有数据态、筛选态。
- 掌握度页面空态、有数据态。
- Dashboard 增强字段为空时不影响旧模块展示。

## 分期建议

Phase 5 可拆为 7 个任务：

1. 后端 `training_mastery` 表、Entity、Mapper、VO、DTO。
2. 后端掌握度统计服务，接入答案批改和标记掌握流程。
3. 后端错题本和掌握度 API。
4. Dashboard 增强 API。
5. 后端测试。
6. 前端类型、API、Store、路由。
7. 前端错题本、掌握度页面和 Dashboard 增强。

## 成功标准

- 用户可以在错题本中看到低分或弱掌握八股题。
- 用户可以按 topic 和掌握度筛选错题。
- 用户可以查看每道错题的错误点、遗漏点、建议和推荐答案。
- 用户可以在掌握度页面看到 topic 和 skillTag 的掌握情况。
- Dashboard 可以展示今日复习题、Top 薄弱知识点和掌握度摘要。
- 算法建议仍不进入错题本和掌握度统计。

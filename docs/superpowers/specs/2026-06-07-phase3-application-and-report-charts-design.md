# Phase 3: 投递管理与增强报告可视化设计

## 背景

Phase 1 已完成求职闭环的基础能力，包括简历导入优化、职位分析、岗位定向面试、面试计时器和基础分析看板。Phase 2 已完成面试复盘增强，包括异步生成优秀回答、逐题诊断、能力雷达、技能缺口和报告对比。

Phase 3 的目标是在正式测试前先搭建两个最小闭环：投递管理和增强报告可视化。投递管理补齐求职流程中的进度跟踪；增强报告可视化将 Phase 2 已有复盘数据用图表表达，提升复盘效率。

## 目标

- 用户可以记录并管理岗位投递进展。
- 用户可以在每条投递下维护多个待办事项。
- 用户可以通过全局待办中心查看、完成、取消完成待办。
- 用户可以在面试报告页看到单次复盘的能力雷达图。
- 用户可以在分析页看到能力雷达、近期分数趋势和技能缺口图表。

## 非目标

- 不做邮件、短信、站内信或桌面通知。
- 不做日历同步。
- 不做招聘平台账号集成或自动投递。
- 不做投递数据批量导入。
- 不新增 AI 生成逻辑；图表只消费 Phase 2 已有数据。
- 不一次性建设复杂图表系统或 BI 看板。

## 投递管理设计

### 数据模型

新增 `job_application` 表，表示一次求职投递记录。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `job_id`: 关联职位，可为空。
- `resume_id`: 关联简历，可为空。
- `company_name`: 公司名。
- `job_title`: 岗位名。
- `source`: 投递渠道，例如 BOSS、拉勾、官网、内推、其他。
- `status`: 投递状态。
- `applied_at`: 投递时间。
- `next_event_at`: 下一次面试、笔试或沟通时间，可为空。
- `salary_range`: 薪资范围，可为空。
- `location`: 工作城市，可为空。
- `contact_name`: 联系人，可为空。
- `contact_info`: 联系方式，可为空。
- `notes`: 备注。
- `create_time` / `update_time` / `is_delete`: 通用字段。

投递状态采用细分版：

- `pending_submit`: 待投递。
- `submitted`: 已投递。
- `hr_contact`: HR 沟通。
- `written_test`: 笔试。
- `first_interview`: 一面。
- `second_interview`: 二面。
- `final_interview`: 终面。
- `offer`: Offer。
- `rejected`: 拒绝。
- `withdrawn`: 放弃。

新增 `application_todo` 表，表示投递相关待办。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `application_id`: 关联投递记录，可为空；为空时表示全局求职待办。
- `title`: 待办标题。
- `description`: 待办说明。
- `priority`: 优先级，`low` / `medium` / `high`。
- `due_at`: 截止时间，可为空。
- `completed`: 是否完成。
- `completed_at`: 完成时间，可为空。
- `create_time` / `update_time` / `is_delete`: 通用字段。

### 后端模块

新增标准分层：

- `ApplicationController`: `/api/application` 前缀。
- `ApplicationService`: 投递记录与待办业务逻辑。
- `JobApplicationMapper` / `ApplicationTodoMapper`: MyBatis-Plus Mapper。
- `JobApplication` / `ApplicationTodo`: Entity。
- DTO：创建投递、更新投递、状态更新、创建待办、更新待办、查询条件。
- VO：投递列表项、投递详情、待办列表项、状态统计。

### API

投递记录 API：

- `POST /api/application`: 创建投递记录。
- `GET /api/application`: 分页查询投递列表，支持状态、关键词、职位、简历筛选。
- `GET /api/application/{id}`: 查询投递详情。
- `PUT /api/application/{id}`: 更新投递记录。
- `PUT /api/application/{id}/status`: 更新投递状态。
- `DELETE /api/application/{id}`: 删除投递记录，同时逻辑删除关联待办。

待办 API：

- `POST /api/application/{applicationId}/todo`: 为投递创建待办。
- `POST /api/application/todo`: 创建全局求职待办。
- `GET /api/application/todo`: 查询待办中心列表，支持完成状态、优先级、逾期筛选。
- `PUT /api/application/todo/{id}`: 更新待办。
- `PUT /api/application/todo/{id}/complete`: 标记完成。
- `PUT /api/application/todo/{id}/reopen`: 取消完成。
- `DELETE /api/application/todo/{id}`: 删除待办。

### 权限与校验

- 所有 API 从 JWT 解析当前用户 ID。
- 查询、更新、删除投递记录时必须校验 `user_id`。
- 查询、更新、删除待办时必须校验 `user_id`。
- 创建投递时如果传入 `job_id` 或 `resume_id`，需要校验资源归属当前用户。
- 创建待办时如果传入 `application_id`，需要校验投递归属当前用户。
- 删除投递使用逻辑删除，并同步逻辑删除关联待办。

### 前端页面

新增路由：

- `/applications`: 投递列表。
- `/applications/new`: 新建投递。
- `/applications/:id`: 投递详情。
- `/applications/todos`: 待办中心。

投递列表页：

- 顶部统计：全部、待投递、已投递、面试中、Offer、失败/放弃。
- 筛选：关键词、状态、城市、渠道。
- 列表项展示公司、岗位、状态、下一事件时间、待办未完成数。
- 支持快速更新状态。

投递详情页：

- 展示投递基础信息。
- 展示状态与关键时间。
- 展示关联职位和简历入口。
- 展示该投递下的待办列表。
- 支持新增待办、完成待办、取消完成、编辑备注。

待办中心页：

- 展示所有未完成和已完成待办。
- 支持按优先级、截止时间、完成状态筛选。
- 展示所属公司/岗位。
- 支持完成、取消完成、删除。

## 增强报告可视化设计

### 图表库

前端新增 `echarts` 依赖。先不引入复杂封装库，使用 Vue 组件生命周期手动初始化和销毁 ECharts 实例。

为了避免重复逻辑，可新增一个轻量 `BaseChart.vue`：

- props 接收 `option`。
- `onMounted` 初始化图表。
- `watch` option 更新图表。
- `ResizeObserver` 或 window resize 保持自适应。
- `onUnmounted` 销毁图表实例。

如果实现阶段发现当前页面内图表数量较少，也可以先做页面内初始化；但推荐 `BaseChart.vue`，便于报告页和分析页复用。

### 报告页图表

在 `InterviewReportPage.vue` 使用已有 `enhancement.radar` 数据新增单次能力雷达图。

展示规则：

- 当增强复盘状态为 `completed` 且 `radar` 非空时展示。
- 维度标签沿用现有映射：技术准确性、表达清晰度、项目深度、岗位匹配度、系统设计。
- 图表旁保留当前文字总结、技能缺口和行动建议。
- 增强复盘生成中、失败或为空时保持现有状态提示。

### 分析页图表

在 `AnalyticsOverviewPage.vue` 使用 `getReviewAnalytics()` 返回的数据新增或替换当前进度条表达：

- 能力雷达图：来自 `reviewAnalytics.radar`。
- 近期分数趋势折线图：来自 `reviewAnalytics.recentScoreTrend`。
- 技能缺口柱状图：来自 `reviewAnalytics.topSkillGaps`。
- 最新行动建议继续用列表展示。

图表数据为空时展示空态，不影响基础分析看板加载。

### 数据来源

不新增后端图表 API。

复用现有接口：

- `GET /api/interview/session/{sessionId}/report/enhancement`。
- `GET /api/statistics/analytics/review`。

## 错误处理

- 投递 API 参数错误返回 `PARAMS_ERROR`。
- 未登录返回 `NOT_LOGIN_ERROR`。
- 访问他人投递或待办返回 `NO_AUTH_ERROR`。
- 投递或待办不存在返回 `NOT_FOUND_ERROR`。
- 图表初始化失败时前端降级为空态或文字内容，不阻塞页面。

可新增错误码：

- `APPLICATION_NOT_FOUND_ERROR(40430)`。
- `APPLICATION_TODO_NOT_FOUND_ERROR(40431)`。

## 测试策略

后端测试：

- 投递创建成功。
- 投递列表按状态筛选。
- 投递状态更新。
- 查询他人投递被拒绝。
- 删除投递同步逻辑删除关联待办。
- 创建投递待办。
- 完成和取消完成待办。
- 待办中心按完成状态和优先级筛选。

前端验证：

- `npm run type-check`。
- `npm run build-only`。
- 手动检查投递列表、投递详情、待办中心、报告图表、分析图表的空态和有数据态。

后端验证：

- `./mvnw.cmd clean package -DskipTests`。
- 有条件时运行相关单元测试。

## 实施顺序

1. 新增数据库表、错误码、Entity、Mapper。
2. 实现投递管理 DTO、VO、Service、Controller。
3. 实现投递管理后端测试。
4. 新增前端投递类型、API、Store。
5. 新增投递列表页、详情页、待办中心页和路由入口。
6. 安装并接入 ECharts。
7. 实现报告页雷达图。
8. 实现分析页雷达图、折线图和柱状图。
9. 执行后端和前端验证。

## 后续扩展

- 投递状态看板视图。
- 面试轮次与投递状态自动关联。
- 待办提醒通知。
- 日历同步。
- 投递转化率、各渠道转化率、城市/公司维度分析。
- 从职位详情直接创建投递记录。

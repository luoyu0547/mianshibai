# 职位供给、AI 推荐与投递管理重构设计

> 日期：2026-06-14  
> 状态：设计完成，待实施

## 背景

现有职位情报模块以用户粘贴单个职位链接为主，用户必须先自行在招聘平台找到职位，再交给系统做 AI 分析。这让平台更像一个职位解析工具，而不是主动帮助用户发现高匹配岗位的求职系统。

现有投递管理也偏记录型表单：用户需要手动录入公司、岗位、薪资、城市、来源、状态和待办。它能保存数据，但对“下一步该做什么”的引导较弱，且职位推荐、职位分析、投递计划之间没有形成顺畅闭环。

本次重构目标是把链路调整为：管理员维护低风险职位来源，系统批量采集公开职位并 AI 分析，后台生成用户推荐池，用户基于简历精排后加入投递计划，再通过 Kanban 推进投递状态。

## 目标

- 管理员可以创建定向职位采集任务，批量获取公开可访问的职位信息。
- 系统可以定时运行采集任务、记录每次运行结果、处理单条失败、去重入库。
- AI 不再只分析用户手动找到的职位，而是为职位库生成可推荐的岗位情报。
- 用户进入职位推荐页即可看到系统推荐的岗位，选择简历后获得更准确的匹配结果。
- 推荐卡片可以一键创建投递计划，自动带入职位信息并生成默认行动建议。
- 投递管理主页面改为 Kanban 看板，减少表单负担，强化流程推进。

## 非目标

- 不做自动投递。
- 不做绕过登录、验证码或反爬限制的招聘平台爬虫。
- 不把 Boss 等招聘平台的大规模抓取稳定性作为首版承诺。
- 不做邮件、短信、桌面通知或日历同步。
- 不一次性引入复杂任务队列或分布式调度系统。
- 不删除现有手动导入职位、手动新建投递和待办中心能力，只调整主入口和主流程。

## 总体闭环

```text
管理员配置采集源
  -> 系统定时采集公开职位
  -> 职位解析、AI 分析、去重入库
  -> 后台生成用户推荐池
  -> 用户选择简历进行精排
  -> 一键加入投递计划
  -> Kanban 推进投递状态
  -> 进入职位详情、简历优化、模拟面试继续准备
```

首版采集边界：

- 支持公司官网招聘页。
- 支持公开职位源、RSS 或公开 API。
- 支持管理员粘贴多条职位 URL。
- 支持管理员配置招聘平台入口 URL 后尝试抓取公开可访问内容，但不承诺绕过限制。

## 方案选择

采用“职位供给闭环重构方案”。

该方案在管理员采集和用户推荐池基础上，继续重构投递体验：推荐卡片直接创建待投递记录，投递管理主页面改成 Kanban，看板卡片内完成状态流转和下一步行动。相比只补充批量导入，它能从根本上把系统从“用户自己找职位并手动记录”改为“平台采集职位、AI 推荐、用户确认投递、看板推进”。

实施上按阶段推进，避免一次性推翻现有模块。

## 后端数据模型

### job_crawl_task

管理员职位采集任务。

核心字段：

- `id`: 主键。
- `name`: 任务名称。
- `source_type`: 来源类型，支持 `company_career_page`、`public_feed`、`manual_url_list`、`platform_entry_url`。
- `source_url`: 来源 URL。对手动链接列表可存主入口或为空，具体 URL 存扩展配置。
- `keywords`: 关键词条件，JSON 或逗号分隔文本。
- `cities`: 城市条件，JSON 或逗号分隔文本。
- `experience_levels`: 经验条件，JSON 或逗号分隔文本。
- `schedule_type`: 调度类型，支持 `manual`、`daily`、`weekly`、`cron`。
- `cron_expression`: cron 表达式，仅 `cron` 类型使用。
- `status`: 任务状态，支持 `enabled`、`disabled`。
- `last_run_at`: 上次运行时间。
- `next_run_at`: 下次运行时间。
- `created_by`: 创建管理员 ID。
- `remark`: 备注。
- `create_time` / `update_time` / `is_delete`: 通用字段。

### job_crawl_run

一次采集执行记录。

核心字段：

- `id`: 主键。
- `task_id`: 采集任务 ID。
- `status`: 运行状态，支持 `running`、`success`、`partial_success`、`failed`。
- `started_at`: 开始时间。
- `finished_at`: 结束时间。
- `total_count`: 总条数。
- `success_count`: 成功条数。
- `duplicate_count`: 去重条数。
- `failed_count`: 失败条数。
- `error_message`: 任务级错误信息。

### job_crawl_item

单条采集结果。

核心字段：

- `id`: 主键。
- `run_id`: 执行记录 ID。
- `task_id`: 采集任务 ID。
- `source_url`: 原始 URL。
- `normalized_url`: 归一化 URL。
- `job_id`: 入库后的职位 ID，可为空。
- `status`: 单条状态，支持 `success`、`duplicate`、`failed`。
- `error_message`: 单条失败原因。
- `raw_title`: 原始标题。
- `raw_company_name`: 原始公司名。

### job_recommendation

用户职位推荐池。

核心字段：

- `id`: 主键。
- `user_id`: 用户 ID。
- `resume_id`: 简历 ID，可为空。
- `job_id`: 职位 ID。
- `stage`: 推荐阶段，支持 `rough`、`refined`。
- `rough_score`: 粗排分。
- `match_id`: 精排后关联的 `job_match` ID，可为空。
- `recommendation`: 推荐结论，沿用 `recommended`、`cautious`、`stretch`、`not_recommended`。
- `reason`: 推荐理由。
- `risk_points`: 风险点，JSON。
- `action_suggestions`: 建议动作，JSON。
- `source`: 推荐来源，支持 `crawl_task`、`manual_import`、`system`。
- `dismissed`: 用户是否忽略。
- `applied`: 是否已加入投递计划。
- `create_time` / `update_time`: 通用字段。

### 现有表扩展

`job` 增加字段：

- `crawl_task_id`: 来源采集任务 ID。
- `crawl_run_id`: 来源运行记录 ID。
- `normalized_fingerprint`: 去重指纹。
- `last_seen_at`: 最近一次被采集到的时间。
- `expire_checked_at`: 最近一次过期检查时间。
- `quality_score`: 职位质量分。

`job_application` 增加字段：

- `recommendation_id`: 来源推荐 ID，用于追踪从哪条推荐加入投递计划。

## 后端服务设计

### AdminJobCrawlService

管理员采集任务服务，负责：

- 创建、更新、查询采集任务。
- 启用、禁用采集任务。
- 手动触发任务运行。
- 查询运行记录和失败 item。
- 校验管理员权限。

### JobCrawlScheduler

周期调度器，负责：

- 扫描 `enabled` 且 `next_run_at` 到期的任务。
- 触发任务运行。
- 防止同一个任务并发运行。
- 根据 `schedule_type` 更新下一次运行时间。

首版使用 Spring 定时任务扫描 due task，不引入复杂任务队列。

### JobBatchCrawlService

批量采集服务，负责：

- 按任务配置解析 URL 列表或公开源内容。
- 批量调用现有 `JobCrawlService`、`JobParseService`、`AiJobAnalysisService`。
- 单条失败记录到 `job_crawl_item`，不阻断整批任务。
- 统计成功、重复、失败数量。

### JobDedupService

职位去重服务，负责：

- 按 `normalized_url` 去重。
- 按公司名、职位名、城市组合去重。
- 按 `normalized_fingerprint` 去重。
- 对重复职位更新 `last_seen_at`，不重复插入。

### JobRecommendationService

推荐服务，负责：

- 为用户生成粗排推荐池。
- 用户选择简历后调用 `ResumeJobMatchService.match(userId, resumeId, jobId)` 精排。
- 持久化推荐理由、风险点和行动建议。
- 忽略推荐。
- 从推荐创建投递记录。

粗排不强制每条调用 AI，优先使用职位质量、城市、薪资、经验、用户资料、简历关键词、历史收藏和历史投递行为。

### JobExpiryService

职位有效性刷新服务，负责：

- 周期检查职位是否仍在公开来源中出现。
- 更新 `job.status` 为 `active`、`expired`、`unknown`。
- 更新 `expire_checked_at`。

## API 设计

### 管理员采集任务 API

- `POST /api/admin/job-crawl/tasks`: 创建采集任务。
- `GET /api/admin/job-crawl/tasks`: 查询采集任务列表。
- `GET /api/admin/job-crawl/tasks/{id}`: 查询采集任务详情。
- `PUT /api/admin/job-crawl/tasks/{id}`: 更新采集任务。
- `PUT /api/admin/job-crawl/tasks/{id}/enable`: 启用任务。
- `PUT /api/admin/job-crawl/tasks/{id}/disable`: 禁用任务。
- `POST /api/admin/job-crawl/tasks/{id}/run`: 立即运行任务。
- `GET /api/admin/job-crawl/tasks/{id}/runs`: 查询任务运行记录。
- `GET /api/admin/job-crawl/runs/{runId}/items`: 查询单次运行 item。

### 用户推荐 API

- `GET /api/job/recommendations`: 查询推荐池。
- `POST /api/job/recommendations/refine`: 根据简历精排推荐。
- `PUT /api/job/recommendations/{id}/dismiss`: 忽略推荐。
- `POST /api/job/recommendations/{id}/apply`: 从推荐创建投递计划。

### 职位库 API 调整

`GET /api/job/list` 从“只返回收藏职位”调整为返回用户可见职位库或推荐相关职位。收藏列表继续使用 `GET /api/job/favorites`。

新增查询条件：

- `city`: 城市。
- `techStack`: 技术栈。
- `recommendation`: 推荐结论。
- `applied`: 是否已投递。
- `dismissed`: 是否已忽略。

## 前端页面设计

### 管理员页面

新增 `/admin/job-crawl`：职位采集任务列表。

展示任务名、来源类型、状态、周期、上次运行、下次运行、成功数量、失败数量。支持新建任务、启停任务、立即运行、查看运行记录。

新增 `/admin/job-crawl/new`：创建采集任务。

不同来源类型展示不同表单：

- `company_career_page`: 公司官网招聘页 URL、关键词、城市。
- `public_feed`: Feed/API URL 和字段说明。
- `manual_url_list`: 多条职位 URL。
- `platform_entry_url`: 入口 URL、关键词、城市，并提示“仅尝试抓取公开可访问内容”。

新增 `/admin/job-crawl/:id`：任务详情。

展示任务配置、最近运行结果、失败 item、入库职位入口，支持重试失败项。

### 用户职位页面

新增 `/job/recommendations`：职位推荐主入口。

页面顶部引导用户选择简历。未选择简历时展示后台粗排推荐；选择简历后可点击“精排推荐”。推荐卡片展示职位、公司、城市薪资、匹配分、推荐结论、AI 理由、风险点、建议动作。

卡片主按钮是“加入投递计划”，次按钮是“查看详情”和“不感兴趣”。

调整 `/job/import`：改名为“手动补充职位”。

保留粘贴链接能力，但从主导航降级为二级入口，适合用户补充少量自己发现的目标职位。

调整 `/job/list`：改为“职位库”。

展示系统采集和用户补充的职位，可筛选城市、技术栈、推荐结论、是否已投递，不再只展示收藏职位。

### 投递管理页面

调整 `/applications`：从列表页改为 Kanban 看板。

看板列：

- `待投递`: `pending_submit`。
- `已投递`: `submitted`。
- `沟通中`: `hr_contact`。
- `笔试/面试`: `written_test`、`first_interview`、`second_interview`、`final_interview`。
- `Offer`: `offer`。
- `已关闭`: `rejected`、`withdrawn`。

卡片展示：

- 公司和职位。
- 来源。
- 匹配分。
- 下一事件。
- 未完成行动数。
- 备注摘要。

卡片快捷动作：

- 推进到下一阶段。
- 设置下一事件。
- 补充备注。
- 查看职位分析。
- 开始模拟面试。

调整 `/applications/new`：降级为“手动新建投递”。

从推荐卡片创建投递时，系统自动带入 `jobId`、`recommendationId`、公司、职位、薪资、城市、来源，默认状态为 `pending_submit`。

### 待办简化

`application_todo` 保留，但不再要求用户主动维护复杂待办中心。

从推荐创建投递时自动生成 2 到 3 个行动建议，例如：

- 根据 JD 优化简历。
- 准备核心技能题。
- 投递后 3 天跟进。

`ApplicationTodoPage` 改为“全部行动”，作为聚合查看页；主流程在 Kanban 卡片内完成。

### 导航调整

- 主导航“职位情报”指向 `/job/recommendations`。
- 职位情报二级入口包括：职位推荐、职位库、我的收藏、手动补充。
- 管理后台新增“职位采集”入口。

## 用户主流程

1. 用户进入职位推荐页。
2. 用户选择默认简历或指定简历。
3. 系统展示粗排推荐，用户可以触发精排。
4. 用户点击“加入投递计划”。
5. 系统创建投递记录和默认行动建议。
6. 用户进入投递看板，通过卡片推进状态。
7. 用户需要准备时，从卡片进入职位详情、简历优化或模拟面试。

## 实施阶段

### 阶段 1：职位采集任务基础设施

新增采集任务、运行记录、采集 item 表。实现管理员任务 CRUD、启停、立即运行、运行记录查询。

首阶段支持 `manual_url_list` 和 `company_career_page`，避免一开始陷入复杂平台适配。

### 阶段 2：批量抓取入库与去重

基于现有 `JobCrawlService`、`JobParseService`、`AiJobAnalysisService` 扩展批处理。实现单 item 失败不影响整批、URL/职位指纹去重、采集结果关联 `job`。

### 阶段 3：推荐池与用户推荐页

新增 `job_recommendation`，后台生成粗排推荐，用户选择简历后精排。前端新增 `/job/recommendations`，支持查看推荐、忽略推荐、从推荐创建投递。

### 阶段 4：投递 Kanban 重构

重构 `/applications` 为看板。保留原列表能力作为筛选和搜索的一种显示方式，或后续确认后下线。从推荐创建投递时自动填充职位信息，并生成默认行动建议。

### 阶段 5：周期调度与职位刷新

实现 `JobCrawlScheduler`、周期运行、`last_seen_at` 更新、过期检测。首版使用 Spring 定时任务扫描 due task。

### 阶段 6：体验收尾

完成导航调整、空态文案、管理员失败重试、职位库筛选、移动端看板横向滚动或按列折叠。

## 迁移兼容

- 现有手动导入职位继续保留，不破坏用户已创建职位。
- 现有投递记录继续显示在新 Kanban，对缺失 `recommendation_id` 的记录标记为“手动创建”。
- 现有待办数据继续保留，显示在卡片和“全部行动”页。
- `GET /api/job/list` 行为改变需要同步前端；收藏列表继续使用 `/api/job/favorites`。
- 不删除 `/job/import` 和 `/applications/new`，只降级为次要入口。

## 错误处理

- 采集任务参数错误返回 `PARAMS_ERROR`。
- 管理员权限不足返回 `NO_AUTH_ERROR`。
- 抓取失败不让整批失败，记录到 `job_crawl_item`。
- AI 解析失败只标记该 item 失败，任务继续执行。
- 从推荐创建投递时，如果已存在同 `user_id + job_id` 投递，直接返回已有投递并提示重复。

## 测试策略

后端测试：

- 采集任务创建成功。
- 非管理员无法创建采集任务。
- 立即运行任务生成 `job_crawl_run`。
- 单 item 失败时任务继续运行。
- URL 和职位指纹去重有效。
- 粗排推荐生成。
- 指定简历精排推荐。
- 从推荐创建投递。
- 重复从推荐创建投递时返回已有投递。

前端测试：

- 推荐页空态、加载态和推荐卡片展示。
- 推荐卡片忽略和加入投递计划。
- 管理员采集任务表单按来源类型切换字段。
- 采集任务列表启停和立即运行。
- 投递 Kanban 按状态分列。
- Kanban 卡片状态推进。

回归测试：

- 职位详情仍可查看。
- 职位收藏仍可使用。
- 手动导入职位仍可使用。
- 投递详情仍可查看。
- 待办完成和取消完成仍可使用。

手动验收路径：

1. 管理员创建官网招聘页任务。
2. 管理员立即运行任务。
3. 系统将职位入库并记录运行结果。
4. 用户进入职位推荐页看到推荐。
5. 用户选择简历精排推荐。
6. 用户将推荐加入投递计划。
7. 用户在投递看板推进状态。

## 风险与处理

- 公开网页结构不稳定：采集 item 记录失败原因，失败不阻断任务。
- AI 成本高：粗排不用每条都调用 AI，批量分析只对成功解析的新职位执行。
- 推荐过多：限制每用户推荐池数量，默认保留最近 50 条未处理推荐。
- 移动端看板拥挤：小屏按阶段折叠展示，不强求桌面式拖拽。
- 招聘平台合规争议：产品文案明确“仅采集公开可访问内容”，平台入口 URL 只做 best-effort 尝试。

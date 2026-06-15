# 职位采集 ReAct Agent 重构设计

## 背景

当前职位采集实现是固定管线：从 `JobCrawlTask.sourceUrl` 得到候选 URL，然后执行抓取、抽取、评分、落库。它没有真正的 ReAct 工具调用能力，也没有 Boss 直聘、实习僧等平台级适配器。现有实现只能证明 Greenhouse 这类公开 ATS 页面可抓取，不能承诺 Boss 直聘等强登录平台能稳定成功。

本设计将职位采集重构为受控 ReAct agent：模型负责规划和选择工具，后端负责工具边界、平台授权、抓取执行、结果校验和落库。Boss 直聘等强登录平台使用服务器端 Playwright 持久化 browser profile，由管理员完成合法登录授权后复用该会话抓取。

## 目标

- 将职位采集从简单固定管线升级为 ReAct agent。
- 为 agent 提供受控工具，完成职位发现、详情抓取、结构化抽取、质量评分和保存。
- 支持 Boss 直聘和实习僧平台适配器。
- Boss 直聘使用管理员授权的服务器端浏览器 profile 进行抓取。
- 实习僧优先公开抓取，必要时使用独立授权 profile。
- 只有真实 live 测试通过后，才声明对应平台真实抓取成功。
- 采集结果进入审核队列，不直接写入正式职位库。

## 非目标

- 不绕过验证码、风控或平台访问控制。
- 不保存平台账号明文密码。
- 不让模型自由访问任意搜索引擎或任意 URL。
- 不把登录页、验证码页、空壳页当作职位详情页抽取。
- 不在没有 live 测试证据时承诺某个平台可真实抓取。

## 推荐方案

采用“ReAct Agent + 平台工具注册表 + Playwright 授权浏览器 profile”。

固定管线方案实现更快，但不满足 ReAct agent 要求。完全模型驱动方案更像 agent，但稳定性和可测试性差。推荐方案保留后端强约束，由模型在受控工具中进行规划，兼顾工具化、稳定性和可验证性。

## 核心组件

### JobSourcingReActAgentService

新的 agent 编排服务，替代当前 `JobSourcingAgentServiceImpl` 的简单循环。它负责创建 `job_crawl_run`、构造 agent 上下文、执行工具调用循环、汇总结果、更新运行状态。

### JobSourcingToolRegistry

注册职位采集 agent 可使用的工具。工具必须是后端白名单中的 Java 服务方法，不能让模型调用任意网络请求。

### JobPlatformAdapter

平台适配器接口，Boss 直聘和实习僧分别实现。接口定义如下：

```java
public interface JobPlatformAdapter {
    String platform();
    boolean requiresAuth();
    PlatformAuthStatus checkAuth();
    List<JobListEntry> discover(JobDiscoveryRequest request);
    FetchedPage fetchDetail(String url);
    ExtractedJobCard fallbackExtract(FetchedPage page);
}
```

### BrowserSessionService

管理服务器端 Playwright persistent context。它负责 profile 路径、授权状态检测、浏览器上下文创建、关闭和重新授权状态更新。

### 平台工具

- `checkPlatformAuth(platform)`：检查平台授权状态。
- `discoverJobs(platform, keywords, cities, experienceLevels, maxPages)`：发现职位列表和详情 URL。
- `fetchJobDetail(platform, url)`：抓取职位详情页。
- `extractJobCard(page)`：抽取结构化职位卡片。
- `scoreJobQuality(card, task)`：按任务目标评分并过滤低质量职位。
- `saveCandidate(card, quality, trace)`：保存待审核采集项。

## 授权流程

Boss 直聘第一次使用前，管理员通过管理端触发授权流程。

1. 管理端调用 `POST /api/admin/job-crawl/platforms/{platform}/auth/start`。
2. 后端启动 Playwright persistent context，profile 路径例如 `backend/browser-profiles/boss/`。
3. 管理员在打开的浏览器中完成登录。
4. 管理端调用 `POST /api/admin/job-crawl/platforms/{platform}/auth/check`。
5. 后端访问平台个人态或职位页，确认不是登录页、验证码页或风控页。
6. 授权成功后记录平台授权状态。

新增表 `platform_auth_session`：

```sql
CREATE TABLE IF NOT EXISTS platform_auth_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  platform VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'not_authorized',
  profile_path VARCHAR(1024) NOT NULL DEFAULT '',
  last_verified_at DATETIME DEFAULT NULL,
  expires_hint_at DATETIME DEFAULT NULL,
  error_message VARCHAR(1024) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台授权会话表';
```

状态值：`not_authorized`、`authorized`、`expired`、`auth_required`、`error`。

## 运行数据流

1. 管理员创建采集任务，指定平台、关键词、城市、经验、目标数量。
2. `JobSourcingReActAgentService` 创建 `job_crawl_run`。
3. Agent 调用 `checkPlatformAuth`。Boss 没有有效授权时，run 进入 `auth_required`，不创建伪成功 item。
4. Agent 调用 `discoverJobs`，平台 adapter 使用 Playwright 打开列表页并提取职位入口。
5. Agent 对候选职位逐个调用 `fetchJobDetail`。
6. Agent 调用 `extractJobCard`，AI 优先，平台规则兜底。
7. Agent 调用 `scoreJobQuality`，低质量职位不进入成功结果。
8. Agent 调用 `saveCandidate`，将高质量职位写入 `job_crawl_item`，审核状态为 `pending_review`。
9. 达到 `targetCount`、页数上限或工具调用上限后结束 run。
10. run 写入 summary 和 cost/trace 信息。

## ReAct 边界

模型可以决定下一步调用哪个工具、是否继续翻页、是否跳过低质量职位。后端必须强制执行以下限制：

- 只允许调用 `JobSourcingToolRegistry` 注册的工具。
- 只允许访问任务配置中的平台和平台 adapter 生成的 URL。
- 禁止搜索引擎结果页 URL。
- 强制 `targetCount`、`maxPagesPerSource`、`maxThirdPartyCalls`。
- 工具输出必须校验，缺少 title、company 或 sourceUrl 的结果不能保存为成功。
- 登录页、验证码页、风控页必须归类为授权失效或平台失败。
- 每次工具调用记录 trace，便于审核和排错。

## 平台适配器

### Boss 直聘

- 必须使用 `BrowserSessionService` 提供的已授权 Playwright profile。
- `checkAuth` 检测页面是否仍是登录页、验证码页或风控页。
- `discover` 根据关键词、城市、经验构造平台列表入口，翻页提取职位卡片和详情 URL。
- `fetchDetail` 打开详情页并提取 DOM 文本和 HTML。
- `fallbackExtract` 从 Boss 详情页规则抽取职位名、公司、城市、薪资、经验、学历、职位描述、技能标签。
- 一旦检测到授权失效，本轮 Boss 任务停止并返回 `auth_required`。

### 实习僧

- 优先使用公开 Playwright 抓取。
- 如果页面需要登录，则使用实习僧独立 profile。
- `discover` 从实习岗位列表提取详情 URL。
- `fallbackExtract` 抽取职位名、公司、城市、薪资、学历、每周天数、实习周期、转正机会和职位要求。

## 数据库和状态

保留现有 `job_crawl_task`、`job_crawl_run`、`job_crawl_item`。新增或扩展：

- 新增 `platform_auth_session` 表。
- `job_crawl_run.status` 增加 `auth_required`。
- `job_crawl_run.cost_json` 存储工具调用统计和 trace 摘要。
- `job_crawl_item.extracted_json` 存储结构化职位卡片。
- 如 trace 过大，后续可新增 `job_crawl_tool_trace` 表；第一阶段优先写入 run summary/costJson，避免过度扩表。

## 错误处理

- 无授权：run 状态 `auth_required`，错误信息提示管理员完成平台授权。
- 授权过期：停止当前平台任务，更新 `platform_auth_session.status=expired`。
- 单个职位详情失败：创建 failed item 或记录到 trace，agent 继续处理其他候选 URL。
- 列表页解析失败：该平台本轮停止，run 标记 failed 或 partial_success。
- 低质量职位：不计入 success，可记录为 skipped/low_quality。
- AI 抽取失败：调用平台规则兜底；兜底仍失败才标记 item failed。

## 测试策略

### 单元测试

- `JobSourcingReActAgentServiceTest`：覆盖无授权、目标数量停止、工具调用上限、低质量过滤。
- `BossJobPlatformAdapterTest`：覆盖登录页识别、列表 DOM 提取、详情 DOM 抽取。
- `ShixisengJobPlatformAdapterTest`：覆盖列表和详情 DOM 抽取。
- `BrowserSessionServiceTest`：覆盖 profile 路径、授权状态更新、授权过期处理。

### 集成测试

使用本地 HTML fixture 或 mock Playwright 页面验证完整流程：

`checkAuth -> discover -> fetchDetail -> extract -> score -> save`

断言 run/item 状态、结构化字段、质量分和 trace 正确。

### Live 测试

- `BossJobSourcingLiveTest`：设置 `RUN_LIVE_BOSS_CRAWL=true`，依赖已授权 Boss browser profile。必须至少抓到 1 个真实 Boss 职位，并断言 title、company、city、sourceUrl、content 非空。
- `ShixisengJobSourcingLiveTest`：设置 `RUN_LIVE_SHIXISENG_CRAWL=true`。必须至少抓到 1 个真实实习僧职位，并断言 title、company、city、sourceUrl、content 非空。

验收口径：没有运行 live 测试，不声明对应平台真实抓取成功。live 测试因未授权跳过，不声明完成。只有 live 测试通过，才声明对应平台真实抓取成功。

## 验收命令

```powershell
.\mvnw.cmd test -pl . "-Dtest=JobSourcingReActAgentServiceTest,BossJobPlatformAdapterTest,ShixisengJobPlatformAdapterTest,BrowserSessionServiceTest"
$env:RUN_LIVE_BOSS_CRAWL='true'; .\mvnw.cmd test -pl . "-Dtest=BossJobSourcingLiveTest"
$env:RUN_LIVE_SHIXISENG_CRAWL='true'; .\mvnw.cmd test -pl . "-Dtest=ShixisengJobSourcingLiveTest"
```

## 实施顺序

1. 新增授权会话模型、表结构和管理端接口。
2. 引入 Playwright 运行环境和 `BrowserSessionService`。
3. 定义 `JobPlatformAdapter`、工具 DTO、工具注册表。
4. 实现 Boss adapter 和实习僧 adapter 的 fixture 测试。
5. 实现 `JobSourcingReActAgentService` 工具调用循环。
6. 接入 AI 抽取和平台规则兜底。
7. 接入审核落库和 trace 记录。
8. 添加 live 测试并用授权 profile 验证真实抓取。

## 风险和缓解

- 平台页面结构变化：adapter 测试使用 fixture，并保留 AI 抽取兜底。
- 授权过期：明确 `auth_required` 状态，引导管理员重新授权。
- 浏览器依赖部署复杂：在配置中增加 browser profile 路径和 headless 开关。
- 模型乱调用工具：工具注册表白名单和后端参数校验强制约束。
- 成本不可控：限制 `maxThirdPartyCalls`、`maxPagesPerSource` 和 `targetCount`。

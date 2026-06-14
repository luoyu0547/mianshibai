# AGENTS.md — mianshiba（面试吧）AI 面试模拟平台

前后端分离 monorepo，各自独立管理依赖。

```
backend/   Spring Boot 3.5.x (Java 17, Maven)
frontend/  Vue 3 (TypeScript, Vite 7, Pinia, Vue Router)
docs/      项目文档
```

## Backend

### 运行命令（backend/ 下执行）

```powershell
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd test -pl . -Dtest=ResultUtilsTest
.\mvnw.cmd test -pl . -Dtest=ResultUtilsTest#testSuccess
```

### 环境变量（application.yml 占位符，均可覆盖）

必需：`MYSQL_HOST`（默认 127.0.0.1）、`MYSQL_PORT`（默认 3306）、`MYSQL_DATABASE`（默认 mianshiba）、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`REDIS_HOST`（默认 127.0.0.1）、`REDIS_PORT`（默认 6379）、`REDIS_PASSWORD`、`AI_DEEPSEEK_API_KEY`、`JWT_SECRET`（至少 32 字节）
非必需：`ALIYUN_AK_ID`、`ALIYUN_AK_SECRET`、`ALIYUN_NLS_APP_KEY`、`ALIYUN_NLS_REGION`（默认 cn-shanghai）

`application-local.yml` 有硬编码密码和 API key，仅 `spring.profiles.active=local` 时生效。

### 架构约定

- 包路径：`com.mianshiba.ai`
- 响应体 `BaseResponse<T>` + `ResultUtils`，错误码 `ErrorCode`，异常 `BusinessException`
- `InfrastructureStartupValidator` 启动校验 MySQL/Redis 连通性（`app.infrastructure.validate-on-startup` 控制）
- 密码 `BCryptPasswordEncoder`（`spring-security-crypto`，未引入完整 Spring Security）
- JWT：JJWT 0.12.6，`spring.security.jwt.secret` + `expiration`（`PT24H`）
- MyBatis-Plus 下划线转驼峰 + 逻辑删除 `is_delete`（0 未删，1 已删）
- 测试不连真实 MySQL/Redis/DeepSeek
- API 文档：`http://localhost:8080/doc.html`（Knife4j）
- 健康检查：`/actuator/health`
- Lombok、Layered API 标准：`controller` → `service`（接口 + `impl/`）→ `mapper`
- AI 调用 Spring AI `ChatClient`（DeepSeek），`AiConfig` 注册 Bean

### 模块概览

| 模块 | 接口前缀 | 关键 Service / 说明 |
|------|----------|---------------------|
| 用户 | `/api/user` | UserService，JWT Bearer Token |
| 简历 | `/api/resume` | ResumeService（CRUD）+ ResumeAiService（AI），每用户最多 10 份 |
| 面试 | `/api/interview` | InterviewService，WebSocket `/ws/interview/asr`，状态机：`created→in_progress→generating_report→completed/cancelled` |
| 职位 | `/api/job` | JobService + JobCrawlService→JobParseService（AI 解析），URL 导入 |
| 投递 | `/api/application` | ApplicationService + ApplicationTodoService，投递全流程管理 |
| 训练 | `/api/training` | TrainingService + TrainingReviewService，AI 刷题 + 评审 |
| 教练 | `/api/coach` | CoachService，AI 求职诊断 + 周计划生成 |
| 管理 | `/api/admin` | AdminService，用户管理 |
| 统计 | `/api/statistics` | StatisticsService + DashboardService，数据看板 |
| AI 分析 | - | AiJobAnalysisService + JobRecommendService + ResumeJobMatchService + ResumeVersionService + CompanyProfileService |
| 报告增强 | - | InterviewReportEnhancementService + InterviewReportCompareService + ReviewAnalyticsService，后台 Worker `InterviewReportEnhancementWorker` |
| 语音 | - | SpeechService / AliyunSpeechServiceImpl（Aliyun NLS TTS+ASR） |

### 数据库（init.sql 自动建表，共 27 张）

用户：`user`
简历：`resume`、`resume_section`（`section_data` JSON 列）、`resume_chat_message`、`resume_version`
面试：`interview_session`、`interview_turn`、`interview_report`、`interview_report_enhancement`、`interview_turn_review`
职位：`job`、`job_analysis`、`job_match`、`job_favorite`
公司：`company`、`company_certification`
投递：`job_application`、`application_todo`
训练：`training_plan`、`training_question`、`training_answer`、`training_answer_review`、`algorithm_recommendation`、`training_mastery`
教练：`coach_diagnosis`、`coach_plan`、`coach_task`

### 面试维度

报告评分：`accuracy`（技术准确性）、`clarity`（表达清晰度）、`depth`（项目深度）、`matching`（岗位匹配度）
轮次类型：`main` / `follow_up`

### 职位匹配维度

`match`（岗位匹配）、`growth`（企业成长性）、`techGrowth`（技术成长）、`salaryCity`（薪资城市）、`experienceFit`（经验适配）
推荐结论：`recommended` / `cautious` / `stretch` / `not_recommended`

### 错误码

```
PARAMS_ERROR(40000)       NOT_LOGIN_ERROR(40100)      NO_AUTH_ERROR(40101)
FORBIDDEN_ERROR(40300)    NOT_FOUND_ERROR(40400)      SYSTEM_ERROR(50000)
RESUME_LIMIT_ERROR(40001) RESUME_SECTION_ERROR(40002)
AI_SERVICE_ERROR(50001)   AI_RESPONSE_PARSE_ERROR(50002)
INTERVIEW_NOT_FOUND_ERROR(40410)  INTERVIEW_STATUS_ERROR(40010)  INTERVIEW_TURN_ERROR(40011)
SPEECH_SERVICE_ERROR(50010)  SPEECH_RECOGNITION_ERROR(50011)  SPEECH_SYNTHESIS_ERROR(50012)
JOB_CRAWL_ERROR(50020)    JOB_PARSE_ERROR(50021)      JOB_NOT_FOUND_ERROR(40420)
COMPANY_NOT_FOUND_ERROR(40421)  JOB_MATCH_ERROR(50022)
```

## Frontend

### 运行命令（frontend/ 下执行）

```powershell
npm install
npm run dev
npm run build          # type-check + build（依赖 npm-run-all2 run-p）
npm run build-only     # 仅 vite build
npm run type-check     # vue-tsc --build
npm run lint           # eslint . --fix --cache
npm run test:unit      # vitest
```

### 关键约定

- Node `^20.19.0 || >=22.12.0`，路径别名 `@` → `./src`
- ESLint flat config（`eslint.config.ts`），Vue + TypeScript + Vitest 规则，自动 fix + 缓存
- 测试在 `src/**/__tests__/`，vitest 环境 jsdom
- 开发 API：`frontend/.env.development`→`VITE_API_BASE_URL=http://localhost:8080`
- 构建产物 `dist/`（已 gitignore）
- Neubrutalism 设计：暖白底 `#FEF9EF`、紫主色 `#6C5CE7`、黑边框 2px + 偏移阴影，CSS 变量在 `src/assets/styles/variables.css`
- 通用组件前缀 `Nb`：`NbButton.vue`、`NbCard.vue`、`NbEmptyState.vue`、`NbLoadingBlock.vue`、`NbPageHeader.vue`、`NbSectionTitle.vue`、`NbStatCard.vue`、`NbStatusBadge.vue`
- 布局：`MainLayout.vue`（登录后）、`AuthLayout.vue`（登录/注册）、`AdminLayout.vue`（管理后台）
- 路由守卫：`meta.requiresAuth` 控制登录，`meta.requiresAdmin` 控制管理员路由

### 路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | `auth/LoginPage.vue` | 公开 |
| `/` | `home/HomePage.vue` | |
| `/profile` | `profile/ProfilePage.vue` | |
| `/resume` | `resume/ResumeListPage.vue` | |
| `/resume/new` | `resume/ResumeEditPage.vue` | |
| `/resume/:id/edit` | `resume/ResumeEditPage.vue` | |
| `/resume/:id/preview` | `resume/ResumePreviewPage.vue` | |
| `/interview` | `interview/InterviewListPage.vue` | |
| `/interview/new` | `interview/InterviewNewPage.vue` | |
| `/interview/:id/room` | `interview/InterviewRoomPage.vue` | |
| `/interview/:id/report` | `interview/InterviewReportPage.vue` | |
| `/job/import` | `job/JobImportPage.vue` | |
| `/job/favorites` | `job/JobFavoritePage.vue` | |
| `/job/list` | `job/JobListPage.vue` | |
| `/job/:id` | `job/JobDetailPage.vue` | |
| `/job/:id/questions` | `job/JobQuestionsPage.vue` | 预测面试题 |
| `/company/:id` | `job/CompanyDetailPage.vue` | |
| `/applications` | `application/ApplicationListPage.vue` | |
| `/applications/new` | `application/ApplicationEditPage.vue` | |
| `/applications/todos` | `application/ApplicationTodoPage.vue` | |
| `/applications/:id` | `application/ApplicationDetailPage.vue` | |
| `/training` | `training/TrainingCenterPage.vue` | |
| `/training/plan/:id` | `training/TrainingPlanDetailPage.vue` | |
| `/training/question/:id` | `training/TrainingQuestionPage.vue` | |
| `/training/mistakes` | `training/TrainingMistakePage.vue` | |
| `/training/mastery` | `training/TrainingMasteryPage.vue` | |
| `/coach` | `coach/CoachHomePage.vue` | |
| `/coach/diagnosis/:id` | `coach/CoachDiagnosisDetailPage.vue` | |
| `/coach/plan/:id` | `coach/CoachPlanDetailPage.vue` | |
| `/analytics` | `analytics/AnalyticsOverviewPage.vue` | |
| `/admin` | `admin/AdminDashboardPage.vue` | 需 admin 角色 |
| `/admin/users` | `admin/AdminUserListPage.vue` | 需 admin 角色 |
| `/admin/users/:id` | `admin/AdminUserDetailPage.vue` | 需 admin 角色 |

### 前端分层

- `src/api/` — 按模块（user, resume, interview, job, application, training, coach, admin, dashboard, statistics）
- `src/stores/` — Pinia Setup Store（对应模块 + admin, application, coach, dashboard, training）
- `src/types/` — 类型定义（对应模块）
- `src/templates/` — 简历模板 3 种（`MinimalTech`、`ModernTwoCol`、`ClassicFormal`）
- `src/components/resume/sections/` — 模块编辑组件（v-model）
- AI 组件：`AiChatPanel`（SSE 流式）、`AiScorePanel`、`VersionHistory`
- PDF 导出：html2canvas + jsPDF
- 图表：ECharts，组件在 `src/components/charts/`
- 音频：`src/utils/audio/`
- 工具：`src/utils/request.ts`（axios）、`src/utils/statusMaps.ts`、`src/utils/date.ts`、`src/utils/text.ts`
- 图表工具：`src/utils/charts/`

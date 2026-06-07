# AGENTS.md — mianshiba（面试吧）AI 面试模拟平台

## 仓库结构

前后端分离的 monorepo（根目录无构建配置，非 Maven/Gradle monorepo）。

```
backend/   Spring Boot 后端（Java 17, Maven）
frontend/  Vue 3 前端（TypeScript, Vite 7, Pinia, Vue Router）
docs/      项目文档（superpowers specs/plans）
```

两个子项目各自管理依赖，不存在共享的根构建命令。

## Backend（Spring Boot 3.5.x）

### 运行命令

```powershell
# 构建（在 backend/ 目录下执行）
.\mvnw.cmd clean package -DskipTests

# 运行
.\mvnw.cmd spring-boot:run

# 全部测试
.\mvnw.cmd test

# 单个测试类
.\mvnw.cmd test -pl . -Dtest=ResultUtilsTest

# 单个测试方法
.\mvnw.cmd test -pl . -Dtest=ResultUtilsTest#testSuccess
```

### 环境变量

`application.yml` 中 MySQL/Redis/DeepSeek/JWT 均使用 `${ENV_VAR}` 占位符，可覆盖。

```
# 启动必需
MYSQL_HOST（默认 127.0.0.1）
MYSQL_PORT（默认 3306）
MYSQL_DATABASE（默认 mianshiba）
MYSQL_USERNAME
MYSQL_PASSWORD
REDIS_HOST（默认 127.0.0.1）
REDIS_PORT（默认 6379）
REDIS_PASSWORD
AI_DEEPSEEK_API_KEY
JWT_SECRET（至少 32 字节）

# 语音服务（Aliyun NLS，非必需）
ALIYUN_AK_ID
ALIYUN_AK_SECRET
ALIYUN_NLS_APP_KEY
ALIYUN_NLS_REGION（默认 cn-shanghai）
```

`application-local.yml` 提供本地开发默认值（含硬编码密码和 API key），仅在 `spring.profiles.active=local` 时生效。

### 关键约定

- 基础包路径：`com.mianshiba.ai`
- 通用响应体 `BaseResponse<T>` + `ResultUtils`，统一错误码 `ErrorCode`，业务异常 `BusinessException`
- `InfrastructureStartupValidator`：启动时校验 MySQL/Redis 连通性（通过 `app.infrastructure.validate-on-startup` 控制）
- 密码使用 `BCryptPasswordEncoder` 加密（`spring-security-crypto`，未引入完整 Spring Security）
- JWT 使用 JJWT 0.12.6，配置 `spring.security.jwt.secret` 和 `spring.security.jwt.expiration`（值 `PT24H`，24 小时）
- 健康检查：`/actuator/health`（Spring Boot Actuator）
- 测试不连接真实 MySQL/Redis/DeepSeek（已 mock）
- API 文档：`http://localhost:8080/doc.html`（Knife4j）
- MyBatis-Plus 下划线转驼峰已开启
- MyBatis-Plus 逻辑删除：`is_delete` 字段（0 未删，1 已删）
- Lombok 全局可用
- 标准分层：`controller` → `service`（接口 + `impl/` 实现）→ `mapper`，DTO/VO/Entity 按模块分包于 `model/`

### 数据库

建表脚本：`backend/src/main/resources/sql/init.sql`（启动时自动执行，含建库和建表语句）。

共 14 张表：

| 模块 | 表 |
|------|------|
| 用户 | `user` |
| 简历 | `resume`、`resume_section`（`section_data` JSON 列）、`resume_chat_message`、`resume_version` |
| 面试 | `interview_session`、`interview_turn`、`interview_report` |
| 职位 | `job`、`job_analysis`、`job_match`、`job_favorite` |
| 公司 | `company`、`company_certification` |

### 用户模块

- 接口前缀：`/api/user`
- 登录态：JWT Bearer Token，前端请求需携带 `Authorization: Bearer <token>`
- 接口：`POST /register`、`POST /login`、`GET /current`、`PUT /profile`

### 简历模块

- 每用户最多 10 份简历（`MAX_RESUMES_PER_USER`）
- 简历 CRUD 接口前缀：`/api/resume`
- AI 接口前缀：`/api/resume/ai` 和 `/api/resume/{id}/ai/`
- AI 功能通过 Spring AI `ChatClient`（DeepSeek）调用，`AiConfig` 注册 `ChatClient` Bean
- AI 功能：一键生成简历、模块优化、简历评分（POST 同步）、AI 对话（SSE 流式 `text/event-stream`）
- Service 分为 `ResumeService`（CRUD）和 `ResumeAiService`（AI 调用），两者独立

### 面试模块

- 接口前缀：`/api/interview`
- WebSocket 端点：`/ws/interview/asr`（语音识别，InterviewAsrWebSocketHandler）
- 流程：创建会话 → 开始面试 → 提交回答（逐轮）→ 生成报告
- 会话状态：`created` → `in_progress` → `generating_report` → `completed`（或 `cancelled`）
- 轮次类型：`main`（主问题）/ `follow_up`（追问）
- 面试报告评分维度：`accuracy`（技术准确性）、`clarity`（表达清晰度）、`depth`（项目深度）、`matching`（岗位匹配度）
- 语音服务：Aliyun NLS（TTS + ASR），通过 `SpeechService` / `AliyunSpeechServiceImpl` 调用

### 职位模块

- 接口前缀：`/api/job`
- 功能：URL 导入解析职位、公司画像、AI 岗位分析、简历匹配、收藏
- 爬取解析链：`JobCrawlService` → `JobParseService`（AI 解析）→ 入库
- 匹配评分维度：`match`（岗位匹配）、`growth`（企业成长性）、`techGrowth`（技术成长）、`salaryCity`（薪资城市）、`experienceFit`（经验适配）
- 推荐结论：`recommended` / `cautious` / `stretch` / `not_recommended`

### 错误码

```
PARAMS_ERROR(40000)          NOT_LOGIN_ERROR(40100)       NO_AUTH_ERROR(40101)
FORBIDDEN_ERROR(40300)       NOT_FOUND_ERROR(40400)       SYSTEM_ERROR(50000)
RESUME_LIMIT_ERROR(40001)    RESUME_SECTION_ERROR(40002)
AI_SERVICE_ERROR(50001)      AI_RESPONSE_PARSE_ERROR(50002)
INTERVIEW_NOT_FOUND_ERROR(40410)  INTERVIEW_STATUS_ERROR(40010)  INTERVIEW_TURN_ERROR(40011)
SPEECH_SERVICE_ERROR(50010)  SPEECH_RECOGNITION_ERROR(50011)  SPEECH_SYNTHESIS_ERROR(50012)
JOB_CRAWL_ERROR(50020)       JOB_PARSE_ERROR(50021)       JOB_NOT_FOUND_ERROR(40420)
COMPANY_NOT_FOUND_ERROR(40421)  JOB_MATCH_ERROR(50022)
```

## Frontend（Vue 3 + TypeScript）

### 运行命令

```powershell
# 在 frontend/ 目录下执行
npm install
npm run dev          # 开发服务器
npm run build        # type-check + build（build 脚本依赖 npm-run-all2 的 run-p，会先执行 type-check）
npm run build-only   # 仅 vite build，跳过类型检查
npm run type-check   # vue-tsc --build
npm run lint         # eslint . --fix --cache
npm run test:unit    # vitest
```

### 关键约定

- Node 要求：`^20.19.0 || >=22.12.0`
- 路径别名：`@` → `./src`
- ESLint flat config（`eslint.config.ts`），含 Vue + TypeScript + Vitest 规则
- 测试文件放 `src/**/__tests__/`，vitest 环境为 jsdom
- 开发环境 API：`frontend/.env.development` 配置 `VITE_API_BASE_URL=http://localhost:8080`
- 构建产物在 `dist/`（已 gitignore）
- Lint 自动 fix（`--fix`），有缓存（`--cache`）
- Neubrutalism 设计系统：暖白底 `#FEF9EF`、紫主色 `#6C5CE7`、黑边框 2px + 偏移阴影，CSS 变量在 `src/assets/styles/variables.css`
- 布局组件：`MainLayout.vue`（登录后主布局）、`AuthLayout.vue`（登录/注册页布局）
- 通用组件前缀 `Nb`：`NbButton.vue`、`NbCard.vue`

### 路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | `auth/LoginPage.vue` | 公开页 |
| `/` | `home/HomePage.vue` | 首页 |
| `/profile` | `profile/ProfilePage.vue` | 个人资料 |
| `/resume` | `resume/ResumeListPage.vue` | 简历列表 |
| `/resume/new` | `resume/ResumeEditPage.vue` | 新建简历 |
| `/resume/:id/edit` | `resume/ResumeEditPage.vue` | 编辑简历 |
| `/resume/:id/preview` | `resume/ResumePreviewPage.vue` | 预览+导出 |
| `/interview` | `interview/InterviewListPage.vue` | 面试列表 |
| `/interview/new` | `interview/InterviewNewPage.vue` | 创建面试 |
| `/interview/:id/room` | `interview/InterviewRoomPage.vue` | 面试房间 |
| `/interview/:id/report` | `interview/InterviewReportPage.vue` | 面试报告 |
| `/job/import` | `job/JobImportPage.vue` | 导入职位 |
| `/job/favorites` | `job/JobFavoritePage.vue` | 收藏列表 |
| `/job/:id` | `job/JobDetailPage.vue` | 职位详情 |
| `/company/:id` | `job/CompanyDetailPage.vue` | 公司画像 |

路由守卫：`meta.requiresAuth` 控制登录态，token 存在但 userInfo 缺失时自动恢复。

### 前端模块

- Store（Pinia Setup Store）：`user.ts`、`resume.ts`、`interview.ts`、`job.ts`
- API 层：`src/api/` 下按模块拆分（`user.ts`、`resume.ts`、`interview.ts`、`job.ts`）
- 类型定义：`src/types/resume.ts` 等
- 简历模板：3 种，位于 `src/templates/`（`MinimalTech`、`ModernTwoCol`、`ClassicFormal`）
- 模块编辑组件：`src/components/resume/sections/`（6 个，v-model 双向绑定）
- AI 组件：`AiChatPanel`（SSE 流式对话）、`AiScorePanel`（评分展示）、`VersionHistory`（版本历史抽屉）
- PDF 导出：html2canvas + jsPDF

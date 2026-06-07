# AGENTS.md — mianshiba（面试吧）AI 面试模拟平台

## 仓库结构

前后端分离的 monorepo（根目录无构建配置，非 Maven/Gradle monorepo）。

```
backend/   Spring Boot 后端（Java 17, Maven）
frontend/  Vue 3 前端（TypeScript, Vite 7, Pinia, Vue Router）
docs/      项目文档
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

### 环境变量（启动必需，缺 MySQL/Redis 会启动失败）

```
MYSQL_USERNAME
MYSQL_PASSWORD
REDIS_PASSWORD
AI_DEEPSEEK_API_KEY
JWT_SECRET（至少 32 字节）
```

注意：`MYSQL_HOST/PORT/DATABASE` 和 `REDIS_HOST/PORT` 在 `application.yml` 中**硬编码**为 `127.0.0.1:3306/mianshiba` 和 `127.0.0.1:6379`，**不是**环境变量占位符。如需覆盖，需直接修改配置或自行添加 `${...}` 占位符。

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

### 用户模块

- 建表脚本：`backend/src/main/resources/sql/init.sql`（启动时自动执行，含建库和建表语句）
- 标准分层：`controller` → `service` → `mapper`，DTO/VO/Entity 分包
- 接口前缀：`/api/user`
- 登录态：JWT Bearer Token，前端请求需携带 `Authorization: Bearer <token>`

### 简历模块

- 4 张表：`resume`（主表）、`resume_section`（模块，`section_data` 为 JSON 列）、`resume_chat_message`（AI 对话）、`resume_version`（版本快照）
- 模块类型：`basic`（基本信息）、`education`（教育）、`work`（工作）、`project`（项目）、`skills`（技能）、`summary`（自我评价）
- 每用户最多 10 份简历（`MAX_RESUMES_PER_USER`）
- 简历 CRUD 接口前缀：`/api/resume`
- AI 接口前缀：`/api/resume/ai` 和 `/api/resume/{id}/ai/`
- AI 功能通过 Spring AI `ChatClient`（DeepSeek）调用，`AiConfig` 注册 `ChatClient` Bean
- AI 功能：一键生成简历、模块优化、简历评分（POST 同步）、AI 对话（SSE 流式 `text/event-stream`）
- 错误码：`NOT_FOUND_ERROR(40400)`、`RESUME_LIMIT_ERROR(40001)`、`RESUME_SECTION_ERROR(40002)`、`AI_SERVICE_ERROR(50001)`、`AI_RESPONSE_PARSE_ERROR(50002)`
- Service 分为 `ResumeService`（CRUD）和 `ResumeAiService`（AI 调用），两者独立

## Frontend（Vue 3 + TypeScript）

### 运行命令

```powershell
# 在 frontend/ 目录下执行
npm install
npm run dev          # 开发服务器
npm run build        # type-check + build（注意：build 脚本依赖 npm-run-all2 的 run-p，会先执行 type-check）
npm run build-only   # 仅 vite build，跳过类型检查
npm run type-check   # vue-tsc --build
npm run lint         # eslint . --fix --cache
npm run test:unit    # vitest
```

### 关键约定

- Node 要求：`^20.19.0 || >=22.12.0`
- 路径别名：`@` → `./src`
- ESLint 配置使用 flat config（`eslint.config.ts`），含 Vue + TypeScript + Vitest 规则
- 测试文件放 `src/**/__tests__/`，vitest 环境为 jsdom
- 前端开发环境通过 `frontend/.env.development` 配置 `VITE_API_BASE_URL=http://localhost:8080`
- 构建产物在 `dist/`（已 gitignore）
- Lint 会自动 fix（`--fix`），有缓存（`--cache`）
- Neubrutalism 设计系统：暖白底 `#FEF9EF`、紫主色 `#6C5CE7`、黑边框 2px + 偏移阴影，CSS 变量在 `src/assets/styles/variables.css`

### 简历模块页面

- 路由：`/resume`（列表）、`/resume/new`（新建）、`/resume/:id/edit`（编辑）、`/resume/:id/preview`（预览）
- 列表页：卡片网格展示简历，支持新建空白/AI 生成两种创建方式
- 编辑页：左右分栏，左侧折叠面板编辑器 + 右侧 A4 实时预览 + AI 对话面板
- 预览页：全屏 A4 预览 + 模板切换 + PDF 导出（html2canvas + jsPDF）
- 3 种简历模板：`MinimalTech`（简约技术风）、`ModernTwoCol`（现代双栏）、`ClassicFormal`（经典正式），位于 `src/templates/`
- 6 个模块编辑组件位于 `src/components/resume/sections/`，均支持 v-model 双向绑定
- AI 面板组件：`AiChatPanel`（SSE 流式对话）、`AiScorePanel`（评分展示）、`VersionHistory`（版本历史抽屉）
- Store：`src/stores/resume.ts`（Pinia Setup Store）
- API：`src/api/resume.ts`
- 类型：`src/types/resume.ts`

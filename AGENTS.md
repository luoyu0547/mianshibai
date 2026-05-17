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
MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USERNAME, MYSQL_PASSWORD
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
AI_DEEPSEEK_API_KEY
```

### 关键约定

- 基础包路径：`com.mianshiba.ai`
- 通用响应体 `BaseResponse<T>` + `ResultUtils`，统一错误码 `ErrorCode`，业务异常 `BusinessException`
- `InfrastructureStartupValidator`：启动时校验 MySQL/Redis 连通性（通过 `app.infrastructure.validate-on-startup` 控制）
- 测试不连接真实 MySQL/Redis/DeepSeek（已 mock）
- API 文档：`http://localhost:8080/doc.html`（Knife4j）
- MyBatis-Plus 下划线转驼峰已开启
- Lombok 全局可用

## Frontend（Vue 3 + TypeScript）

### 运行命令

```powershell
# 在 frontend/ 目录下执行
npm install
npm run dev          # 开发服务器
npm run build        # type-check + build（注意：build 脚本会先执行 type-check）
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
- 构建产物在 `dist/`（已 gitignore）
- Lint 会自动 fix（`--fix`），有缓存（`--cache`）

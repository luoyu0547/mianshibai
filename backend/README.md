# mianshiba-ai-backend

Java AI 业务后端初始化骨架，使用 Spring Boot、Spring AI 和 DeepSeek。

## 技术栈

- Java 17
- Maven
- Spring Boot 3.5.x
- Spring AI 1.1.2
- Spring AI DeepSeek Chat Model
- MyBatis-Plus
- MySQL
- Redis
- Knife4j/OpenAPI
- Lombok

## 骨架范围

当前项目只包含通用后端基础能力：

- 统一响应结构：`BaseResponse<T>`
- 统一响应工具：`ResultUtils`
- 统一错误码：`ErrorCode`
- 业务异常：`BusinessException`
- 全局异常处理：`GlobalExceptionHandler`
- MySQL 与 Redis 启动校验：`InfrastructureStartupValidator`
- Spring AI DeepSeek 依赖与配置

初始化阶段不包含聊天接口、聊天 Service、业务 DTO、业务 VO、Entity、Mapper 或业务表结构。

## 用户模块

首版用户模块提供账号密码注册、登录、JWT 登录态、获取当前用户和更新用户资料能力。

建表脚本：

```text
src/main/resources/sql/create_user_table.sql
```

接口：

```text
POST /api/user/register
POST /api/user/login
GET /api/user/current
PUT /api/user/profile
```

登录成功后，前端需要在后续请求中携带：

```text
Authorization: Bearer <token>
```

## 环境变量

应用启动需要 MySQL、Redis 和 DeepSeek 配置。缺少 MySQL 或 Redis 时，启动失败是预期行为。

```powershell
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="mianshiba"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_mysql_password"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="your_redis_password"
$env:AI_DEEPSEEK_API_KEY="your_deepseek_api_key"
$env:JWT_SECRET="test-jwt-secret-key-must-be-at-least-32-bytes"
```

## 本地运行

```powershell
.\mvnw.cmd spring-boot:run
```

健康检查：

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```

接口文档：

```text
http://localhost:8080/doc.html
```

## 测试

```powershell
.\mvnw.cmd test
```

自动化测试不访问真实 MySQL、Redis 或 DeepSeek。

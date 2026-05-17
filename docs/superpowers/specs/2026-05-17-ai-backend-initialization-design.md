# AI 后端初始化设计

## 目标

初始化一个可运行的 Java AI 业务后端骨架，技术栈使用 Spring Boot、Spring AI、Spring AI Alibaba 依赖管理和 DeepSeek Chat Model。第一个里程碑只完成项目标准化基础设施：依赖管理、环境配置、统一响应结构、统一异常处理、基础设施启动校验、测试骨架和 README，不生成聊天接口、聊天 Service 或其他具体业务功能。

## 项目现状

当前仓库除 Git 元数据、设计文档和实现计划外没有应用代码。没有现有构建文件、后端源码、测试代码或项目约定需要兼容。

## 选定方案

采用 Spring AI Alibaba 兼容优先的依赖管理，同时默认模型配置为 DeepSeek：

- 构建工具：Maven
- Java：17
- Spring Boot：3.4.x
- Group ID：`com.mianshiba`
- Artifact ID：`mianshiba-ai-backend`
- 根包名：`com.mianshiba.ai`
- 默认模型：`deepseek-chat`

Context7 调研显示 Spring AI 官方文档已有 `org.springframework.ai:spring-ai-bom:2.0.0` 示例，但 Spring AI Alibaba 稳定文档当前与 Spring AI `1.1.2` 以及 Spring AI Alibaba `1.1.2.x` 系列依赖对齐。本项目初始化阶段优先选择依赖兼容性，因此使用 Spring AI `1.1.2`，并通过 Spring AI 官方 DeepSeek starter 准备 DeepSeek 能力。

## 依赖策略

Maven 构建将导入以下 BOM 做版本对齐：

- `org.springframework.ai:spring-ai-bom:1.1.2`
- `com.alibaba.cloud.ai:spring-ai-alibaba-bom:1.1.2.0`
- `com.alibaba.cloud.ai:spring-ai-alibaba-extensions-bom:1.1.2.1`

初始化依赖包括：

- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.springframework.boot:spring-boot-starter-actuator`
- `org.springframework.boot:spring-boot-starter-aop`
- `org.springframework.boot:spring-boot-starter-data-redis`
- `org.springframework.ai:spring-ai-starter-model-deepseek`
- `com.baomidou:mybatis-plus-spring-boot3-starter`
- `com.mysql:mysql-connector-j`
- `com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter`
- `org.apache.commons:commons-lang3`
- `org.projectlombok:lombok`
- `org.springframework.boot:spring-boot-devtools`
- `org.springframework.boot:spring-boot-configuration-processor`
- `org.springframework.boot:spring-boot-starter-test`

Spring AI Alibaba BOM 会保留用于后续 Alibaba 生态扩展，但初始化阶段不引入 DashScope/Qwen starter，也不生成任何 AI 业务调用接口。

## 基础设施启动策略

应用启动时要求 MySQL 和 Redis 可用。没有 MySQL 或 Redis 时，应用启动失败是预期行为。

配置通过环境变量读取：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `AI_DEEPSEEK_API_KEY`

测试代码通过测试属性关闭外部基础设施校验，避免自动化测试强依赖真实 MySQL、Redis 和 DeepSeek 网络调用。

## 应用结构

初始项目使用标准 Spring Boot 结构，并只生成通用基础代码：

- `pom.xml`
- `.gitignore`
- `README.md`
- `src/main/java/com/mianshiba/ai/MianshibaAiBackendApplication.java`
- `src/main/java/com/mianshiba/ai/common/BaseResponse.java`
- `src/main/java/com/mianshiba/ai/common/ResultUtils.java`
- `src/main/java/com/mianshiba/ai/config/InfrastructureStartupValidator.java`
- `src/main/java/com/mianshiba/ai/exception/BusinessException.java`
- `src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
- `src/main/java/com/mianshiba/ai/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.yml`
- `src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`
- `src/test/java/com/mianshiba/ai/common/ResultUtilsTest.java`
- `src/test/java/com/mianshiba/ai/config/InfrastructureStartupValidatorTest.java`
- `src/test/java/com/mianshiba/ai/exception/GlobalExceptionHandlerTest.java`

初始化阶段不创建 Controller、Service、DTO、VO、Entity、Mapper 或业务测试。后续开发业务模块时，再按 `controller`、`service`、`service.impl`、`model.dto`、`model.vo`、`model.entity` 等包结构扩展。

## 统一响应标准

所有后续 HTTP API 默认使用统一响应结构：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

通用组件职责：

- `BaseResponse<T>`：统一响应对象，包含 `code`、`data`、`message`。
- `ResultUtils`：统一成功与失败响应构造入口。
- `ErrorCode`：定义基础错误码，后续业务错误码在此扩展或拆分。
- `BusinessException`：承载业务异常状态码与消息。
- `GlobalExceptionHandler`：统一处理参数校验异常、业务异常和未预期异常。

## AI 配置边界

DeepSeek 凭据通过环境变量提供，不提交任何密钥到仓库：

- `AI_DEEPSEEK_API_KEY`

Spring AI DeepSeek 配置使用以下前缀：

- `spring.ai.deepseek`
- `spring.ai.deepseek.chat`

默认模型配置为 `deepseek-chat`。初始化阶段只完成依赖和配置，不创建任何调用 DeepSeek 的业务接口或业务服务。

## 错误处理

初始化阶段加入最小统一异常模型：

- 参数校验异常返回 `PARAMS_ERROR`。
- 业务异常返回异常携带的错误码和消息。
- 未预期异常返回 `SYSTEM_ERROR`，并记录错误日志。

错误响应沿用 `BaseResponse` 结构，保证接口响应形态一致。

## 测试与验证

初始化验证包括：

- Spring Boot 上下文加载测试。
- `ResultUtils` 单元测试，验证成功与失败响应结构。
- `GlobalExceptionHandler` 单元测试，验证业务异常和系统异常响应。
- `InfrastructureStartupValidator` 单元测试，通过 mock 验证 MySQL 和 Redis 校验逻辑。
- 使用 Maven 执行依赖解析与测试。
- 在 `README.md` 中提供手动运行说明，包括配置 MySQL、Redis 和 `AI_DEEPSEEK_API_KEY`。

自动化测试不调用真实 DeepSeek 服务，因为初始化阶段不生成 AI 业务调用接口。

## 不在本次范围内

以下内容明确不属于初始化里程碑：

- 聊天接口、聊天 Service 或任何 AI 业务调用代码。
- 用户认证与授权。
- 业务数据库表结构。
- Entity、Mapper、DTO、VO 等业务对象。
- Redis 业务缓存设计。
- 向量库与 RAG 流程。
- 多 Agent 工作流。
- Prompt 模板与 Prompt 版本管理。
- 流式响应。
- Docker 部署文件。

## 成功标准

满足以下条件即认为初始化完成：

- 项目具备标准 Maven Spring Boot 结构。
- 项目使用 Java 17 和 Spring Boot 3.4.x。
- 已声明与 Spring AI Alibaba 兼容的 BOM 和后端常用依赖。
- 默认 AI 模型配置使用 DeepSeek，而不是 Qwen/DashScope。
- MySQL、Redis 和 DeepSeek 密钥通过环境变量读取，不写入仓库文件。
- 已提供统一响应结构和统一异常处理标准。
- 没有生成聊天接口、聊天 Service 或其他具体业务功能。
- 应用可以编译并运行测试。
- 配置有效 MySQL、Redis 和 DeepSeek API Key 后，应用可以启动并通过健康检查。

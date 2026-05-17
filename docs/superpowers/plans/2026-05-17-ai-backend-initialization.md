# AI Backend Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 初始化一个 Java 17、Maven、Spring Boot 3.4.x 业务后端骨架，提供统一响应、统一异常、基础依赖和 DeepSeek 配置，但不生成任何聊天业务功能。

**Architecture:** 使用标准 Spring Boot 单体后端结构。初始化阶段只创建启动类、通用响应、全局异常、基础设施启动校验、配置文件、测试和 README；后续业务模块再按标准包结构扩展 Controller、Service、DTO、VO、Entity 和 Mapper。

**Tech Stack:** Java 17、Maven、Spring Boot 3.4.x、Spring AI 1.1.2、Spring AI Alibaba BOM 1.1.2.x、Spring AI DeepSeek、MyBatis-Plus、MySQL、Redis、Knife4j/OpenAPI、Lombok、JUnit 5、Mockito、AssertJ。

---

## 文件结构

- Create: `.gitignore`，忽略 Java、Maven、IDE 和构建产物。
- Create: `pom.xml`，声明 Spring Boot parent、Java 17、Spring AI 与 Spring AI Alibaba BOM、DeepSeek 和常用后端依赖。
- Create: `mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`，由 Spring Initializr 生成 Maven Wrapper。
- Create: `src/main/java/com/mianshiba/ai/MianshibaAiBackendApplication.java`，Spring Boot 启动类。
- Create: `src/main/java/com/mianshiba/ai/common/BaseResponse.java`，统一响应对象。
- Create: `src/main/java/com/mianshiba/ai/common/ResultUtils.java`，统一响应构造工具。
- Create: `src/main/java/com/mianshiba/ai/config/InfrastructureStartupValidator.java`，应用启动时校验 MySQL 与 Redis 连接。
- Create: `src/main/java/com/mianshiba/ai/exception/BusinessException.java`，业务异常。
- Create: `src/main/java/com/mianshiba/ai/exception/ErrorCode.java`，基础错误码。
- Create: `src/main/java/com/mianshiba/ai/exception/GlobalExceptionHandler.java`，全局异常处理。
- Create: `src/main/resources/application.yml`，配置应用名、DeepSeek、MySQL、Redis、MyBatis-Plus、Actuator 和 Knife4j。
- Create: `src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`，验证 Spring Boot 上下文可加载。
- Create: `src/test/java/com/mianshiba/ai/common/ResultUtilsTest.java`，验证统一响应工具。
- Create: `src/test/java/com/mianshiba/ai/config/InfrastructureStartupValidatorTest.java`，验证启动基础设施校验逻辑。
- Create: `src/test/java/com/mianshiba/ai/exception/GlobalExceptionHandlerTest.java`，验证全局异常响应结构。
- Create: `README.md`，说明版本选择、环境变量、启动方式、测试命令和文档地址。

## 约束

- AI 相关依赖必须使用 Spring AI，不引入其他 AI 框架。
- 默认模型配置必须是 DeepSeek，不使用 Qwen 或 DashScope starter。
- 初始化阶段不生成聊天接口、聊天 Service、业务 DTO、业务 VO、Entity、Mapper 或业务表结构。
- 应用正常启动时必须能连接 MySQL 和 Redis。
- 自动化测试不连接真实 MySQL、Redis 或 DeepSeek。
- 当前会话没有收到用户明确提交请求，执行本计划时不要运行 `git commit`。

### Task 1: 生成 Spring Boot 项目骨架

**Files:**
- Create: `.gitignore`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `pom.xml`
- Create: `src/main/java/com/mianshiba/ai/MianshibaAiBackendApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`

- [ ] **Step 1: 使用 Spring Initializr 生成基础项目**

Run from repository root:

```powershell
$zipPath = Join-Path $env:TEMP "mianshiba-ai-backend.zip"
$extractPath = Join-Path $env:TEMP "mianshiba-ai-backend"
if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
if (Test-Path -LiteralPath $extractPath) { Remove-Item -LiteralPath $extractPath -Recurse -Force }
Invoke-WebRequest -Uri "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=3.4.12&baseDir=mianshiba-ai-backend&groupId=com.mianshiba&artifactId=mianshiba-ai-backend&name=mianshiba-ai-backend&description=AI%20backend%20for%20Mianshiba&packageName=com.mianshiba.ai&packaging=jar&javaVersion=17&dependencies=web,validation,actuator,aop,data-redis,mysql,lombok,devtools" -OutFile $zipPath
Expand-Archive -LiteralPath $zipPath -DestinationPath $extractPath -Force
Copy-Item -Path (Join-Path $extractPath "mianshiba-ai-backend\*") -Destination . -Recurse -Force
```

Expected: repository root contains `pom.xml`, Maven Wrapper files, `src/main`, and `src/test`.

- [ ] **Step 2: 检查生成结构**

Run:

```powershell
Test-Path -LiteralPath "pom.xml"
Test-Path -LiteralPath "src/main/java/com/mianshiba/ai/MianshibaAiBackendApplication.java"
Test-Path -LiteralPath "src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java"
```

Expected: all three commands print `True`.

### Task 2: 配置 Maven 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 替换 `pom.xml` 内容**

Write exactly:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.12</version>
        <relativePath/>
    </parent>

    <groupId>com.mianshiba</groupId>
    <artifactId>mianshiba-ai-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>mianshiba-ai-backend</name>
    <description>AI backend for Mianshiba</description>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.1.2</spring-ai.version>
        <spring-ai-alibaba.version>1.1.2.0</spring-ai-alibaba.version>
        <spring-ai-alibaba-extensions.version>1.1.2.1</spring-ai-alibaba-extensions.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <knife4j.version>4.5.0</knife4j.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.alibaba.cloud.ai</groupId>
                <artifactId>spring-ai-alibaba-bom</artifactId>
                <version>${spring-ai-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud.ai</groupId>
                <artifactId>spring-ai-alibaba-extensions-bom</artifactId>
                <version>${spring-ai-alibaba-extensions.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-deepseek</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
            <version>${knife4j.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                        <path>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-configuration-processor</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 解析依赖**

Run:

```powershell
.\mvnw.cmd -q dependency:resolve
```

Expected: command exits with code `0`.

### Task 3: 配置应用启动参数

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 写入 `application.yml`**

Write exactly:

```yaml
spring:
  application:
    name: mianshiba-ai-backend
  ai:
    deepseek:
      api-key: ${AI_DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
      timeout: 5s

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    banner: false

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

knife4j:
  enable: true
  setting:
    language: zh_cn

app:
  infrastructure:
    validate-on-startup: true
```

- [ ] **Step 2: 验证 YAML 文件存在**

Run:

```powershell
Test-Path -LiteralPath "src/main/resources/application.yml"
```

Expected: prints `True`.

### Task 4: 添加统一响应与异常处理

**Files:**
- Create: `src/main/java/com/mianshiba/ai/common/BaseResponse.java`
- Create: `src/main/java/com/mianshiba/ai/common/ResultUtils.java`
- Create: `src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
- Create: `src/main/java/com/mianshiba/ai/exception/BusinessException.java`
- Create: `src/main/java/com/mianshiba/ai/exception/GlobalExceptionHandler.java`
- Create: `src/test/java/com/mianshiba/ai/common/ResultUtilsTest.java`
- Create: `src/test/java/com/mianshiba/ai/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: 先写统一响应工具测试**

Create `src/test/java/com/mianshiba/ai/common/ResultUtilsTest.java`:

```java
package com.mianshiba.ai.common;

import com.mianshiba.ai.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultUtilsTest {

    @Test
    void successReturnsStandardResponse() {
        BaseResponse<String> response = ResultUtils.success("ok-data");

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData()).isEqualTo("ok-data");
        assertThat(response.getMessage()).isEqualTo("ok");
    }

    @Test
    void errorReturnsStandardResponse() {
        BaseResponse<Void> response = ResultUtils.error(ErrorCode.PARAMS_ERROR);

        assertThat(response.getCode()).isEqualTo(40000);
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("请求参数错误");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
.\mvnw.cmd test -Dtest=ResultUtilsTest
```

Expected: FAIL because `BaseResponse`, `ResultUtils`, and `ErrorCode` do not exist.

- [ ] **Step 3: 写入 `ErrorCode`**

Create `src/main/java/com/mianshiba/ai/exception/ErrorCode.java`:

```java
package com.mianshiba.ai.exception;

import lombok.Getter;

/**
 * 基础错误码
 */
@Getter
public enum ErrorCode {

    /**
     * 请求成功
     */
    SUCCESS(0, "ok"),

    /**
     * 请求参数错误
     */
    PARAMS_ERROR(40000, "请求参数错误"),

    /**
     * 系统内部异常
     */
    SYSTEM_ERROR(50000, "系统内部异常");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 4: 写入 `BusinessException`**

Create `src/main/java/com/mianshiba/ai/exception/BusinessException.java`:

```java
package com.mianshiba.ai.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
```

- [ ] **Step 5: 写入 `BaseResponse`**

Create `src/main/java/com/mianshiba/ai/common/BaseResponse.java`:

```java
package com.mianshiba.ai.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应对象
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应对象")
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码
     */
    @Schema(description = "业务状态码")
    private int code;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 响应消息
     */
    @Schema(description = "响应消息")
    private String message;
}
```

- [ ] **Step 6: 写入 `ResultUtils`**

Create `src/main/java/com/mianshiba/ai/common/ResultUtils.java`:

```java
package com.mianshiba.ai.common;

import com.mianshiba.ai.exception.ErrorCode;

/**
 * 统一响应工具
 */
public final class ResultUtils {

    private ResultUtils() {
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), data, ErrorCode.SUCCESS.getMessage());
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }
}
```

- [ ] **Step 7: 写入 `GlobalExceptionHandler`**

Create `src/main/java/com/mianshiba/ai/exception/GlobalExceptionHandler.java`:

```java
package com.mianshiba.ai.exception;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        // 1. 提取参数校验错误信息
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());

        // 2. 返回统一错误响应
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        // 1. 记录约束校验异常
        log.warn("Constraint violation", exception);

        // 2. 返回统一错误响应
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException exception) {
        // 1. 记录业务异常
        log.warn("Business exception: code={}, message={}", exception.getCode(), exception.getMessage());

        // 2. 返回统一错误响应
        return ResultUtils.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception exception) {
        // 1. 记录未预期异常
        log.error("Unexpected exception", exception);

        // 2. 返回统一错误响应
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}
```

- [ ] **Step 8: 写入全局异常处理测试**

Create `src/test/java/com/mianshiba/ai/exception/GlobalExceptionHandlerTest.java`:

```java
package com.mianshiba.ai.exception;

import com.mianshiba.ai.common.BaseResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionReturnsBusinessError() {
        BusinessException exception = new BusinessException(ErrorCode.PARAMS_ERROR, "参数无效");

        BaseResponse<Void> response = globalExceptionHandler.handleBusinessException(exception);

        assertThat(response.getCode()).isEqualTo(40000);
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("参数无效");
    }

    @Test
    void handleExceptionReturnsSystemError() {
        BaseResponse<Void> response = globalExceptionHandler.handleException(new RuntimeException("boom"));

        assertThat(response.getCode()).isEqualTo(50000);
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("系统内部异常");
    }
}
```

- [ ] **Step 9: 运行统一响应与异常测试确认通过**

Run:

```powershell
.\mvnw.cmd test -Dtest=ResultUtilsTest,GlobalExceptionHandlerTest
```

Expected: Maven finishes with `BUILD SUCCESS`.

### Task 5: 添加启动基础设施校验

**Files:**
- Create: `src/main/java/com/mianshiba/ai/config/InfrastructureStartupValidator.java`
- Create: `src/test/java/com/mianshiba/ai/config/InfrastructureStartupValidatorTest.java`

- [ ] **Step 1: 先写基础设施校验测试**

Create `src/test/java/com/mianshiba/ai/config/InfrastructureStartupValidatorTest.java`:

```java
package com.mianshiba.ai.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfrastructureStartupValidatorTest {

    @Test
    void runPassesWhenMysqlAndRedisAreAvailable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        InfrastructureStartupValidator validator = new InfrastructureStartupValidator(dataSource, redisConnectionFactory);

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
.\mvnw.cmd test -Dtest=InfrastructureStartupValidatorTest
```

Expected: FAIL because `InfrastructureStartupValidator` does not exist.

- [ ] **Step 3: 写入 `InfrastructureStartupValidator`**

Create `src/main/java/com/mianshiba/ai/config/InfrastructureStartupValidator.java`:

```java
package com.mianshiba.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 基础设施启动校验器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.infrastructure", name = "validate-on-startup", havingValue = "true", matchIfMissing = true)
public class InfrastructureStartupValidator implements ApplicationRunner {

    private final DataSource dataSource;

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. 校验 MySQL 连接可用
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(3)) {
                throw new IllegalStateException("MySQL connection is not valid");
            }
        }

        // 2. 校验 Redis 连接可用
        try (RedisConnection redisConnection = redisConnectionFactory.getConnection()) {
            String pong = redisConnection.ping();
            if (!StringUtils.equalsIgnoreCase("PONG", pong)) {
                throw new IllegalStateException("Redis connection ping failed: " + pong);
            }
        }

        // 3. 记录基础设施校验结果
        log.info("Infrastructure startup validation passed");
    }
}
```

- [ ] **Step 4: 运行基础设施校验测试确认通过**

Run:

```powershell
.\mvnw.cmd test -Dtest=InfrastructureStartupValidatorTest
```

Expected: Maven finishes with `BUILD SUCCESS`.

### Task 6: 添加启动测试并跑全量验证

**Files:**
- Modify: `src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`

- [ ] **Step 1: 写入启动测试**

Write `src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java` exactly:

```java
package com.mianshiba.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.infrastructure.validate-on-startup=false",
        "spring.ai.deepseek.api-key=test-api-key",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class MianshibaAiBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: 运行全部测试**

Run:

```powershell
.\mvnw.cmd test
```

Expected: Maven finishes with `BUILD SUCCESS`.

### Task 7: 添加 README

**Files:**
- Create: `README.md`

- [ ] **Step 1: 写入 `README.md`**

Write exactly:

````markdown
# mianshiba-ai-backend

Java AI 业务后端初始化骨架，使用 Spring Boot、Spring AI 和 DeepSeek。

## 技术栈

- Java 17
- Maven
- Spring Boot 3.4.x
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
````

- [ ] **Step 2: 运行全部测试**

Run:

```powershell
.\mvnw.cmd test
```

Expected: Maven finishes with `BUILD SUCCESS`.

- [ ] **Step 3: 检查工作区变更**

Run:

```powershell
git status --short
```

Expected: generated backend files, spec file, and plan file are visible as untracked or modified files.

## 自检结果

- Spec coverage: 计划覆盖 Maven 项目结构、Java 17、Spring Boot 3.4.x、Spring AI `1.1.2`、Spring AI Alibaba BOM、DeepSeek starter、常用后端依赖、MySQL/Redis 强制启动校验、统一响应、全局异常、基础测试和 README 说明。
- Placeholder scan: 未发现占位标记或未定义的实现步骤。
- Type consistency: `BaseResponse<T>`、`ResultUtils.success(T)`、`ResultUtils.error(ErrorCode)`、`BusinessException`、`GlobalExceptionHandler` 与测试中的调用保持一致。
- AI framework check: 计划只使用 Spring AI 依赖和配置，不引入其他 AI 框架依赖。
- Business scope check: 计划不创建聊天接口、聊天 Service、业务 DTO、业务 VO、Entity、Mapper 或业务表结构。

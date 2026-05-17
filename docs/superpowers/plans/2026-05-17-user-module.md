# User Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `backend/` 中实现首版用户模块，支持账号密码注册、登录返回 JWT、获取当前用户、更新基础资料和轻量求职画像。

**Architecture:** 使用 Spring Boot 标准分层，Controller 负责 HTTP 和统一响应，Service 负责用户业务规则，Mapper 负责 MyBatis-Plus 数据访问，JWT 工具只负责 token 生成和解析。首版不启用完整 Spring Security 过滤链，不使用 Redis 保存登录态。

**Tech Stack:** Java 17、Spring Boot 3.5.x、MyBatis-Plus、MySQL、Jakarta Validation、BCrypt、JJWT、JUnit 5、Mockito、MockMvc、AssertJ。

---

## 执行约束

- 在 `backend/` 目录执行 Maven 命令。
- 不连接真实 MySQL、Redis 或 DeepSeek 执行自动化测试。
- 当前会话没有用户明确提交请求，执行本计划时不要运行 `git commit`。
- 每个任务结束用 `git status --short` 作为检查点。
- 手动编辑文件时使用 `apply_patch`。

## 文件结构

- Modify: `backend/pom.xml`，增加 BCrypt 和 JJWT 依赖。
- Modify: `backend/src/main/resources/application.yml`，增加 JWT 配置。
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`，增加登录鉴权相关错误码。
- Modify: `backend/src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`，为新增 Bean 补充测试配置。
- Create: `backend/src/main/resources/sql/create_user_table.sql`，保存用户表建表脚本。
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/User.java`，用户实体。
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/UserMapper.java`，MyBatis-Plus Mapper。
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserRegisterRequest.java`，注册请求。
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserLoginRequest.java`，登录请求。
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserUpdateProfileRequest.java`，资料更新请求。
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/LoginUserVO.java`，脱敏用户信息。
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/UserLoginVO.java`，登录响应。
- Create: `backend/src/main/java/com/mianshiba/ai/config/SecurityConfig.java`，提供 `BCryptPasswordEncoder`。
- Create: `backend/src/main/java/com/mianshiba/ai/utils/JwtUtils.java`，JWT 生成、解析和请求头 token 提取。
- Create: `backend/src/main/java/com/mianshiba/ai/service/UserService.java`，用户业务接口。
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/UserServiceImpl.java`，用户业务实现。
- Create: `backend/src/main/java/com/mianshiba/ai/controller/UserController.java`，用户接口。
- Create: `backend/src/test/java/com/mianshiba/ai/utils/JwtUtilsTest.java`，JWT 单元测试。
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/UserServiceImplTest.java`，用户业务单元测试。
- Create: `backend/src/test/java/com/mianshiba/ai/controller/UserControllerTest.java`，用户接口 MVC 测试。
- Modify: `backend/README.md`，补充 JWT 环境变量、SQL 和接口说明。

### Task 1: 依赖与配置

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`

- [ ] **Step 1: 修改 Maven 依赖**

在 `backend/pom.xml` 的 `<properties>` 中加入 JJWT 版本：

```xml
<jjwt.version>0.12.6</jjwt.version>
```

在 `<dependencies>` 中加入以下依赖，放在 `commons-lang3` 依赖之后：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: 修改 JWT 配置**

在 `backend/src/main/resources/application.yml` 的 `spring:` 节点下增加：

```yaml
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: PT24H
```

- [ ] **Step 3: 修改 Spring Boot 上下文测试配置**

将 `backend/src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java` 改为：

```java
package com.mianshiba.ai;

import com.mianshiba.ai.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "app.infrastructure.validate-on-startup=false",
        "spring.ai.deepseek.api-key=test-api-key",
        "spring.security.jwt.secret=test-jwt-secret-key-must-be-at-least-32-bytes",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class MianshibaAiBackendApplicationTests {

    @MockBean
    private UserMapper userMapper;

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: 解析依赖**

Run from `backend/`:

```powershell
./mvnw.cmd -q dependency:resolve
```

Expected: 命令退出码为 `0`。

- [ ] **Step 5: 检查工作区**

Run from repo root:

```powershell
git status --short
```

Expected: 只出现本任务修改的配置文件和之前已写入的文档文件。

### Task 2: 表结构、Entity 与 Mapper

**Files:**
- Create: `backend/src/main/resources/sql/create_user_table.sql`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/User.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/UserMapper.java`

- [ ] **Step 1: 创建 SQL 建表脚本**

Create `backend/src/main/resources/sql/create_user_table.sql`:

```sql
CREATE TABLE user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 id',
  user_account VARCHAR(32) NOT NULL COMMENT '登录账号',
  user_password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  user_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户昵称',
  user_avatar VARCHAR(512) NOT NULL DEFAULT '' COMMENT '用户头像 URL',
  user_role VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  user_status TINYINT NOT NULL DEFAULT 0 COMMENT '用户状态：0-正常，1-禁用',
  email VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱，后续扩展使用',
  phone VARCHAR(32) NOT NULL DEFAULT '' COMMENT '手机号，后续扩展使用',
  target_position VARCHAR(128) NOT NULL DEFAULT '' COMMENT '目标岗位，如 Java 后端工程师',
  tech_direction VARCHAR(128) NOT NULL DEFAULT '' COMMENT '技术方向，如 Java/Spring Boot/AI 应用',
  work_years TINYINT NOT NULL DEFAULT 0 COMMENT '工作年限，0 表示应届/无经验',
  city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '期望/所在城市',
  job_status VARCHAR(32) NOT NULL DEFAULT '' COMMENT '求职状态，如 looking/open/not_looking',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_account (user_account),
  KEY idx_target_position (target_position),
  KEY idx_tech_direction (tech_direction),
  KEY idx_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

- [ ] **Step 2: 创建用户实体**

Create `backend/src/main/java/com/mianshiba/ai/model/entity/User.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
@Schema(description = "用户实体")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userAccount;

    private String userPassword;

    private String userName;

    private String userAvatar;

    private String userRole;

    private Integer userStatus;

    private String email;

    private String phone;

    private String targetPosition;

    private String techDirection;

    private Integer workYears;

    private String city;

    private String jobStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

- [ ] **Step 3: 创建 UserMapper**

Create `backend/src/main/java/com/mianshiba/ai/mapper/UserMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 4: 编译验证**

Run from `backend/`:

```powershell
./mvnw.cmd -q -DskipTests compile
```

Expected: 命令退出码为 `0`。

- [ ] **Step 5: 检查工作区**

Run from repo root:

```powershell
git status --short
```

Expected: 新增 SQL、Entity、Mapper 文件。

### Task 3: DTO、VO、错误码与密码 Bean

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserRegisterRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserLoginRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserUpdateProfileRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/LoginUserVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/UserLoginVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`

- [ ] **Step 1: 创建注册请求 DTO**

Create `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserRegisterRequest.java`:

```java
package com.mianshiba.ai.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度必须为 4-32 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须为 8-64 位")
    private String userPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 64, message = "确认密码长度必须为 8-64 位")
    private String checkPassword;
}
```

- [ ] **Step 2: 创建登录请求 DTO**

Create `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserLoginRequest.java`:

```java
package com.mianshiba.ai.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求
 */
@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度必须为 4-32 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须为 8-64 位")
    private String userPassword;
}
```

- [ ] **Step 3: 创建资料更新 DTO**

Create `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserUpdateProfileRequest.java`:

```java
package com.mianshiba.ai.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户资料更新请求
 */
@Data
@Schema(description = "用户资料更新请求")
public class UserUpdateProfileRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String userName;

    @Size(max = 512, message = "头像 URL 长度不能超过 512 位")
    private String userAvatar;

    @Size(max = 128, message = "目标岗位长度不能超过 128 位")
    private String targetPosition;

    @Size(max = 128, message = "技术方向长度不能超过 128 位")
    private String techDirection;

    @Min(value = 0, message = "工作年限不能小于 0")
    @Max(value = 60, message = "工作年限不能大于 60")
    private Integer workYears;

    @Size(max = 64, message = "城市长度不能超过 64 位")
    private String city;

    @Pattern(regexp = "^$|looking|open|not_looking", message = "求职状态不合法")
    private String jobStatus;
}
```

- [ ] **Step 4: 创建脱敏用户 VO**

Create `backend/src/main/java/com/mianshiba/ai/model/vo/LoginUserVO.java`:

```java
package com.mianshiba.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录用户脱敏信息
 */
@Data
@Schema(description = "登录用户脱敏信息")
public class LoginUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userRole;
    private Integer userStatus;
    private String email;
    private String phone;
    private String targetPosition;
    private String techDirection;
    private Integer workYears;
    private String city;
    private String jobStatus;
    private LocalDateTime createTime;
}
```

- [ ] **Step 5: 创建登录响应 VO**

Create `backend/src/main/java/com/mianshiba/ai/model/vo/UserLoginVO.java`:

```java
package com.mianshiba.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应")
public class UserLoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;

    private LoginUserVO user;
}
```

- [ ] **Step 6: 创建密码加密 Bean**

Create `backend/src/main/java/com/mianshiba/ai/config/SecurityConfig.java`:

```java
package com.mianshiba.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全相关 Bean 配置
 */
@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 7: 扩展错误码**

Replace `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java` with:

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
     * 未登录
     */
    NOT_LOGIN_ERROR(40100, "未登录"),

    /**
     * 无权限
     */
    NO_AUTH_ERROR(40101, "无权限"),

    /**
     * 禁止访问
     */
    FORBIDDEN_ERROR(40300, "禁止访问"),

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

- [ ] **Step 8: 编译验证**

Run from `backend/`:

```powershell
./mvnw.cmd -q -DskipTests compile
```

Expected: 命令退出码为 `0`。

- [ ] **Step 9: 检查工作区**

Run from repo root:

```powershell
git status --short
```

Expected: 新增 DTO、VO、SecurityConfig，且 `ErrorCode.java` 已修改。

### Task 4: JWT 工具

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/utils/JwtUtilsTest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/utils/JwtUtils.java`

- [ ] **Step 1: 写 JWT 失败测试**

Create `backend/src/test/java/com/mianshiba/ai/utils/JwtUtilsTest.java`:

```java
package com.mianshiba.ai.utils;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Test
    void generateTokenAndParseTokenReturnsClaims() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        String token = jwtUtils.generateToken(1001L, "developer_001", "user");
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);

        assertThat(token).isNotBlank();
        assertThat(claims.userId()).isEqualTo(1001L);
        assertThat(claims.userAccount()).isEqualTo("developer_001");
        assertThat(claims.userRole()).isEqualTo("user");
    }

    @Test
    void parseTokenThrowsWhenTokenInvalid() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        assertThatThrownBy(() -> jwtUtils.parseToken("bad-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_LOGIN_ERROR.getCode());
    }

    @Test
    void resolveTokenReturnsBearerToken() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        String token = jwtUtils.resolveToken("Bearer abc.def.ghi");

        assertThat(token).isEqualTo("abc.def.ghi");
    }

    @Test
    void resolveTokenThrowsWhenHeaderMissing() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        assertThatThrownBy(() -> jwtUtils.resolveToken(""))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_LOGIN_ERROR.getCode());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=JwtUtilsTest test
```

Expected: 编译失败，错误包含 `cannot find symbol` 和 `JwtUtils`。

- [ ] **Step 3: 实现 JWT 工具**

Create `backend/src/main/java/com/mianshiba/ai/utils/JwtUtils.java`:

```java
package com.mianshiba.ai.utils;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具
 */
@Component
public class JwtUtils {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String USER_ACCOUNT_CLAIM = "userAccount";
    private static final String USER_ROLE_CLAIM = "userRole";

    private final SecretKey secretKey;
    private final Duration expiration;

    public JwtUtils(@Value("${spring.security.jwt.secret}") String secret,
                    @Value("${spring.security.jwt.expiration:PT24H}") Duration expiration) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String userAccount, String userRole) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(USER_ACCOUNT_CLAIM, userAccount)
                .claim(USER_ROLE_CLAIM, userRole)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public JwtUserClaims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtUserClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(USER_ACCOUNT_CLAIM, String.class),
                    claims.get(USER_ROLE_CLAIM, String.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }

    public String resolveToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        String token = authorizationHeader.substring(TOKEN_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return token;
    }

    public record JwtUserClaims(Long userId, String userAccount, String userRole) {
    }
}
```

- [ ] **Step 4: 运行 JWT 测试确认通过**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=JwtUtilsTest test
```

Expected: 测试通过，退出码为 `0`。

- [ ] **Step 5: 检查工作区**

Run from repo root:

```powershell
git status --short
```

Expected: 新增 `JwtUtils.java` 和 `JwtUtilsTest.java`。

### Task 5: 用户 Service

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/UserServiceImplTest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/UserService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 写用户 Service 失败测试**

Create `backend/src/test/java/com/mianshiba/ai/service/impl/UserServiceImplTest.java`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Mock
    private UserMapper userMapper;

    private BCryptPasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtUtils);
    }

    @Test
    void registerCreatesUserWithEncryptedPassword() {
        UserRegisterRequest request = registerRequest("developer_001", "password123", "password123");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1001L);
            return 1;
        });

        Long userId = userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User savedUser = captor.getValue();
        assertThat(userId).isEqualTo(1001L);
        assertThat(savedUser.getUserAccount()).isEqualTo("developer_001");
        assertThat(savedUser.getUserPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getUserPassword())).isTrue();
        assertThat(savedUser.getUserRole()).isEqualTo("user");
        assertThat(savedUser.getUserStatus()).isZero();
    }

    @Test
    void registerThrowsWhenAccountExists() {
        UserRegisterRequest request = registerRequest("developer_001", "password123", "password123");
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已存在")
                .extracting("code")
                .isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerThrowsWhenPasswordsDifferent() {
        UserRegisterRequest request = registerRequest("developer_001", "password123", "password456");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("两次输入的密码不一致")
                .extracting("code")
                .isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginReturnsTokenAndMaskedUser() {
        User user = normalUser();
        user.setUserPassword(passwordEncoder.encode("password123"));
        when(userMapper.selectOne(any())).thenReturn(user);

        UserLoginVO loginVO = userService.login(loginRequest("developer_001", "password123"));

        assertThat(loginVO.getToken()).isNotBlank();
        assertThat(loginVO.getUser().getId()).isEqualTo(1001L);
        assertThat(loginVO.getUser().getUserAccount()).isEqualTo("developer_001");
    }

    @Test
    void loginThrowsWhenPasswordWrong() {
        User user = normalUser();
        user.setUserPassword(passwordEncoder.encode("password123"));
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login(loginRequest("developer_001", "wrongpass")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号或密码错误")
                .extracting("code")
                .isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    void loginThrowsWhenUserDisabled() {
        User user = normalUser();
        user.setUserPassword(passwordEncoder.encode("password123"));
        user.setUserStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login(loginRequest("developer_001", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN_ERROR.getCode());
    }

    @Test
    void getCurrentUserReturnsMaskedUser() {
        User user = normalUser();
        when(userMapper.selectById(1001L)).thenReturn(user);
        String authorization = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");

        LoginUserVO loginUserVO = userService.getCurrentUser(authorization);

        assertThat(loginUserVO.getId()).isEqualTo(1001L);
        assertThat(loginUserVO.getUserAccount()).isEqualTo("developer_001");
    }

    @Test
    void updateProfileUpdatesAllowedFields() {
        User user = normalUser();
        when(userMapper.selectById(1001L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        String authorization = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        UserUpdateProfileRequest request = new UserUpdateProfileRequest();
        request.setUserName("后端开发者");
        request.setTargetPosition("Java 后端工程师");
        request.setTechDirection("Java/Spring Boot/AI 应用");
        request.setWorkYears(3);
        request.setCity("上海");
        request.setJobStatus("looking");

        LoginUserVO loginUserVO = userService.updateProfile(authorization, request);

        assertThat(loginUserVO.getUserName()).isEqualTo("后端开发者");
        assertThat(loginUserVO.getTargetPosition()).isEqualTo("Java 后端工程师");
        assertThat(loginUserVO.getWorkYears()).isEqualTo(3);
        verify(userMapper).updateById(user);
    }

    private UserRegisterRequest registerRequest(String account, String password, String checkPassword) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount(account);
        request.setUserPassword(password);
        request.setCheckPassword(checkPassword);
        return request;
    }

    private UserLoginRequest loginRequest(String account, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserAccount(account);
        request.setUserPassword(password);
        return request;
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("developer_001");
        user.setUserName("开发者");
        user.setUserAvatar("");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setEmail("");
        user.setPhone("");
        user.setTargetPosition("");
        user.setTechDirection("");
        user.setWorkYears(0);
        user.setCity("");
        user.setJobStatus("");
        user.setIsDelete(0);
        return user;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=UserServiceImplTest test
```

Expected: 编译失败，错误包含 `cannot find symbol` 和 `UserServiceImpl`。

- [ ] **Step 3: 创建 UserService 接口**

Create `backend/src/main/java/com/mianshiba/ai/service/UserService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;

/**
 * 用户服务
 */
public interface UserService {

    Long register(UserRegisterRequest request);

    UserLoginVO login(UserLoginRequest request);

    LoginUserVO getCurrentUser(String authorizationHeader);

    LoginUserVO updateProfile(String authorizationHeader, UserUpdateProfileRequest request);
}
```

- [ ] **Step 4: 实现 UserServiceImpl**

Create `backend/src/main/java/com/mianshiba/ai/service/impl/UserServiceImpl.java`:

```java
package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.service.UserService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    private static final Set<String> VALID_JOB_STATUSES = Set.of("", "looking", "open", "not_looking");
    private static final String DEFAULT_USER_ROLE = "user";
    private static final int USER_STATUS_NORMAL = 0;
    private static final int USER_STATUS_DISABLED = 1;
    private static final int NOT_DELETED = 0;

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterRequest request) {
        validateRegisterRequest(request);
        Long count = userMapper.selectCount(Wrappers.lambdaQuery(User.class)
                .eq(User::getUserAccount, request.getUserAccount()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        User user = new User();
        user.setUserAccount(request.getUserAccount());
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
        user.setUserName("");
        user.setUserAvatar("");
        user.setUserRole(DEFAULT_USER_ROLE);
        user.setUserStatus(USER_STATUS_NORMAL);
        user.setEmail("");
        user.setPhone("");
        user.setTargetPosition("");
        user.setTechDirection("");
        user.setWorkYears(0);
        user.setCity("");
        user.setJobStatus("");
        user.setIsDelete(NOT_DELETED);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public UserLoginVO login(UserLoginRequest request) {
        validateAccountAndPassword(request == null ? null : request.getUserAccount(),
                request == null ? null : request.getUserPassword());
        User user = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .eq(User::getUserAccount, request.getUserAccount())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        ensureAvailableUser(user);
        String token = jwtUtils.generateToken(user.getId(), user.getUserAccount(), user.getUserRole());
        return new UserLoginVO(token, toLoginUserVO(user));
    }

    @Override
    public LoginUserVO getCurrentUser(String authorizationHeader) {
        User user = getAvailableUserByAuthorization(authorizationHeader);
        return toLoginUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUserVO updateProfile(String authorizationHeader, UserUpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = getAvailableUserByAuthorization(authorizationHeader);
        setIfNotNull(user::setUserName, request.getUserName());
        setIfNotNull(user::setUserAvatar, request.getUserAvatar());
        setIfNotNull(user::setTargetPosition, request.getTargetPosition());
        setIfNotNull(user::setTechDirection, request.getTechDirection());
        if (request.getWorkYears() != null) {
            user.setWorkYears(request.getWorkYears());
        }
        setIfNotNull(user::setCity, request.getCity());
        if (request.getJobStatus() != null) {
            String jobStatus = request.getJobStatus().trim();
            if (!VALID_JOB_STATUSES.contains(jobStatus)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "求职状态不合法");
            }
            user.setJobStatus(jobStatus);
        }
        userMapper.updateById(user);
        return toLoginUserVO(user);
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        validateAccountAndPassword(request == null ? null : request.getUserAccount(),
                request == null ? null : request.getUserPassword());
        if (!request.getUserPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    private void validateAccountAndPassword(String userAccount, String userPassword) {
        if (!StringUtils.hasText(userAccount) || !ACCOUNT_PATTERN.matcher(userAccount).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号格式不合法");
        }
        if (!StringUtils.hasText(userPassword) || userPassword.length() < 8 || userPassword.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须为 8-64 位");
        }
    }

    private User getAvailableUserByAuthorization(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        ensureAvailableUser(user);
        return user;
    }

    private void ensureAvailableUser(User user) {
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(USER_STATUS_DISABLED).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
    }

    private void setIfNotNull(Consumer<String> setter, String value) {
        if (value != null) {
            setter.accept(value.trim());
        }
    }

    private LoginUserVO toLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserName(user.getUserName());
        loginUserVO.setUserAvatar(user.getUserAvatar());
        loginUserVO.setUserRole(user.getUserRole());
        loginUserVO.setUserStatus(user.getUserStatus());
        loginUserVO.setEmail(user.getEmail());
        loginUserVO.setPhone(user.getPhone());
        loginUserVO.setTargetPosition(user.getTargetPosition());
        loginUserVO.setTechDirection(user.getTechDirection());
        loginUserVO.setWorkYears(user.getWorkYears());
        loginUserVO.setCity(user.getCity());
        loginUserVO.setJobStatus(user.getJobStatus());
        loginUserVO.setCreateTime(user.getCreateTime());
        return loginUserVO;
    }
}
```

- [ ] **Step 5: 运行用户 Service 测试确认通过**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=UserServiceImplTest test
```

Expected: 测试通过，退出码为 `0`。

- [ ] **Step 6: 运行 JWT 与用户 Service 测试**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=JwtUtilsTest,UserServiceImplTest test
```

Expected: 测试通过，退出码为 `0`。

- [ ] **Step 7: 检查工作区**

Run from repo root:

```powershell
git status --short
```

Expected: 新增 Service 接口、实现和测试文件。

### Task 6: 用户 Controller

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/controller/UserControllerTest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/UserController.java`

- [ ] **Step 1: 写 Controller 失败测试**

Create `backend/src/test/java/com/mianshiba/ai/controller/UserControllerTest.java`:

```java
package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerReturnsUserId() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount("developer_001");
        request.setUserPassword("password123");
        request.setCheckPassword("password123");
        when(userService.register(any(UserRegisterRequest.class))).thenReturn(1001L);

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1001));
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserAccount("developer_001");
        request.setUserPassword("password123");
        LoginUserVO loginUserVO = loginUserVO();
        when(userService.login(any(UserLoginRequest.class))).thenReturn(new UserLoginVO("token-value", loginUserVO));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("token-value"))
                .andExpect(jsonPath("$.data.user.userAccount").value("developer_001"));
    }

    @Test
    void currentReturnsLoginUser() throws Exception {
        String authorization = "Bearer token-value";
        when(userService.getCurrentUser(authorization)).thenReturn(loginUserVO());

        mockMvc.perform(get("/api/user/current")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userAccount").value("developer_001"));
    }

    @Test
    void updateProfileReturnsUpdatedUser() throws Exception {
        String authorization = "Bearer token-value";
        UserUpdateProfileRequest request = new UserUpdateProfileRequest();
        request.setUserName("后端开发者");
        request.setTargetPosition("Java 后端工程师");
        LoginUserVO loginUserVO = loginUserVO();
        loginUserVO.setUserName("后端开发者");
        loginUserVO.setTargetPosition("Java 后端工程师");
        when(userService.updateProfile(eq(authorization), any(UserUpdateProfileRequest.class))).thenReturn(loginUserVO);

        mockMvc.perform(put("/api/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userName").value("后端开发者"))
                .andExpect(jsonPath("$.data.targetPosition").value("Java 后端工程师"));
    }

    @Test
    void registerReturnsParamsErrorWhenAccountInvalid() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount("bad-account!");
        request.setUserPassword("password123");
        request.setCheckPassword("password123");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    private LoginUserVO loginUserVO() {
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(1001L);
        loginUserVO.setUserAccount("developer_001");
        loginUserVO.setUserName("开发者");
        loginUserVO.setUserRole("user");
        loginUserVO.setUserStatus(0);
        loginUserVO.setWorkYears(0);
        loginUserVO.setJobStatus("");
        return loginUserVO;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=UserControllerTest test
```

Expected: 编译失败，错误包含 `cannot find symbol` 和 `UserController`。

- [ ] **Step 3: 实现 UserController**

Create `backend/src/main/java/com/mianshiba/ai/controller/UserController.java`:

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "用户接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public BaseResponse<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResultUtils.success(userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public BaseResponse<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        return ResultUtils.success(userService.login(request));
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前用户")
    public BaseResponse<LoginUserVO> current(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(userService.getCurrentUser(authorizationHeader));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新用户资料")
    public BaseResponse<LoginUserVO> updateProfile(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                                   @Valid @RequestBody UserUpdateProfileRequest request) {
        return ResultUtils.success(userService.updateProfile(authorizationHeader, request));
    }
}
```

- [ ] **Step 4: 运行 Controller 测试确认通过**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=UserControllerTest test
```

Expected: 测试通过，退出码为 `0`。

- [ ] **Step 5: 运行用户模块测试集合**

Run from `backend/`:

```powershell
./mvnw.cmd -q -Dtest=JwtUtilsTest,UserServiceImplTest,UserControllerTest test
```

Expected: 测试通过，退出码为 `0`。

- [ ] **Step 6: 检查工作区**

Run from repo root:

```powershell
git status --short
```

Expected: 新增 Controller 和 Controller 测试文件。

### Task 7: README 与全量验证

**Files:**
- Modify: `backend/README.md`

- [ ] **Step 1: 更新 README 环境变量**

在 `backend/README.md` 的环境变量代码块中增加：

```powershell
$env:JWT_SECRET="test-jwt-secret-key-must-be-at-least-32-bytes"
```

- [ ] **Step 2: 更新 README 用户模块说明**

在 `backend/README.md` 的骨架范围后新增用户模块说明：

```markdown
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
```

- [ ] **Step 3: 运行全部后端测试**

Run from `backend/`:

```powershell
./mvnw.cmd test
```

Expected: 全部测试通过，退出码为 `0`。

- [ ] **Step 4: 运行后端打包验证**

Run from `backend/`:

```powershell
./mvnw.cmd clean package -DskipTests
```

Expected: 打包成功，退出码为 `0`。

- [ ] **Step 5: 检查敏感信息**

Run from repo root:

```powershell
git diff -- backend/src/main/resources/application.yml backend/README.md
```

Expected: 没有真实密钥，只有 `${JWT_SECRET}` 或示例密钥。

- [ ] **Step 6: 最终工作区检查**

Run from repo root:

```powershell
git status --short
```

Expected: 显示本用户模块相关新增和修改文件，未出现无关文件修改。

## Self-Review

Spec coverage:

- 用户表和索引由 Task 2 覆盖，`job_status` 不建单列索引。
- 注册、登录、当前用户、资料更新由 Task 5 和 Task 6 覆盖。
- BCrypt、JWT、错误码和配置由 Task 1、Task 3、Task 4 覆盖。
- README、SQL、测试和全量验证由 Task 7 覆盖。

Placeholder scan:

- 本计划不包含占位标记或未定义文件路径。
- 每个代码步骤均给出文件路径和代码内容。

Type consistency:

- DTO、VO、Service、Controller 和测试中的方法名一致。
- `JwtUtils.JwtUserClaims` 在 Task 4 定义，并在 Task 5 使用。
- `UserLoginVO` 同时出现在分层、登录接口、Service 测试和 Controller 测试中。

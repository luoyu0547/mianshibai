# 用户模块设计

## 目标

在现有 Spring Boot 后端骨架中完成第一个用户模块设计，为后续程序员专用的面试简历优化平台和工作推荐能力提供稳定的用户身份入口。首版用户模块需要支持账号密码注册、账号密码登录、JWT 登录态、获取当前用户、维护基础用户资料，并在 `user` 表中保留少量求职画像字段，用于后续推荐冷启动。

## 项目现状

当前 `backend/` 已完成基础骨架，包括统一响应 `BaseResponse<T>`、响应工具 `ResultUtils`、错误码 `ErrorCode`、业务异常 `BusinessException`、全局异常处理 `GlobalExceptionHandler`、MySQL/Redis 启动校验、MyBatis-Plus、Knife4j 和测试基础。尚未创建任何业务 Controller、Service、Entity、Mapper、DTO、VO 或业务表结构。

## 选定方案

采用适中演进方案：`user` 单表承载首版账号身份、展示资料、管理字段和轻量求职画像字段。简历正文、技能明细、求职偏好、推荐记录等后续能力不放入首版 `user` 表，等业务边界明确后再单独建表。

核心取舍：

- 登录凭证首版使用 `user_account + user_password`，后续再扩展邮箱、手机号或第三方登录。
- 登录态使用 JWT access token，适配前后端分离架构，不引入刷新 token、Redis 黑名单或完整 Spring Security 过滤链。
- 密码哈希使用 `spring-security-crypto` 中的 `BCryptPasswordEncoder`，不引入完整 `spring-boot-starter-security`。
- JWT 使用 `io.jsonwebtoken` 的 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 组合实现签名、生成和解析。
- 求职画像只保留目标岗位、技术方向、工作年限、城市、求职状态，满足推荐冷启动，不提前建完整画像体系。
- 用户状态、角色、逻辑删除和审计时间作为管理扩展点保留。

## 表结构

表名使用 `user`。字段使用下划线命名，依赖 MyBatis-Plus 下划线转驼峰配置映射到 Java 字段。

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

索引策略：

- `uk_user_account` 支撑注册唯一性和登录查询，必须保留。
- `target_position`、`tech_direction`、`city` 是后续工作推荐候选召回的可能过滤条件，首版保留单列索引。
- `job_status` 基数低，首版不建单列索引。后续如果出现稳定高频组合查询，再基于真实查询模式设计组合索引，例如 `(city, target_position, tech_direction)`。

## 后端分层

在 `com.mianshiba.ai` 下扩展标准业务分层：

```text
com.mianshiba.ai
├── controller
│   └── UserController
├── service
│   ├── UserService
│   └── impl/UserServiceImpl
├── mapper
│   └── UserMapper
├── model
│   ├── entity/User
│   ├── dto/user/UserRegisterRequest
│   ├── dto/user/UserLoginRequest
│   ├── dto/user/UserUpdateProfileRequest
│   ├── vo/LoginUserVO
│   └── vo/UserLoginVO
├── config
│   └── SecurityConfig
└── utils
    └── JwtUtils
```

职责边界：

- `UserController` 只接收 HTTP 请求、触发参数校验、调用 Service，并使用 `ResultUtils` 返回统一响应。
- `UserService` 定义注册、登录、获取当前用户、更新资料等业务能力。
- `UserServiceImpl` 负责账号唯一性、密码哈希校验、用户状态校验、脱敏 VO 构造等业务规则。
- `UserMapper` 继承 MyBatis-Plus `BaseMapper<User>`。
- `JwtUtils` 负责生成、解析和校验 JWT，不直接访问数据库。
- `SecurityConfig` 提供 `BCryptPasswordEncoder` 等安全相关 Bean，不启用完整 Spring Security 认证过滤链。

## 依赖调整

需要在 `backend/pom.xml` 增加以下依赖：

- `org.springframework.security:spring-security-crypto`：提供 `BCryptPasswordEncoder`。
- `io.jsonwebtoken:jjwt-api`：JWT API。
- `io.jsonwebtoken:jjwt-impl`：JWT 运行时实现，`runtime` scope。
- `io.jsonwebtoken:jjwt-jackson`：JWT JSON 序列化支持，`runtime` scope。

不引入 `spring-boot-starter-security`，避免默认安全过滤链影响现有接口和测试。

## API 设计

所有接口使用现有统一响应结构：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

### 注册

```text
POST /api/user/register
```

请求体：

```json
{
  "userAccount": "developer_001",
  "userPassword": "password123",
  "checkPassword": "password123"
}
```

响应数据：用户 `id`。

### 登录

```text
POST /api/user/login
```

请求体：

```json
{
  "userAccount": "developer_001",
  "userPassword": "password123"
}
```

响应数据包含 `token` 和脱敏后的 `user`。

### 获取当前用户

```text
GET /api/user/current
Authorization: Bearer <token>
```

响应数据为脱敏后的 `LoginUserVO`。

### 更新用户资料

```text
PUT /api/user/profile
Authorization: Bearer <token>
```

请求体：

```json
{
  "userName": "后端开发者",
  "userAvatar": "https://example.com/avatar.png",
  "targetPosition": "Java 后端工程师",
  "techDirection": "Java/Spring Boot/AI 应用",
  "workYears": 3,
  "city": "上海",
  "jobStatus": "looking"
}
```

响应数据为更新后的脱敏 `LoginUserVO`。

## 登录态与安全

密码安全：

- 注册时使用 `BCryptPasswordEncoder` 生成不可逆哈希。
- 登录时使用 `BCryptPasswordEncoder#matches` 校验密码。
- 数据库、日志和响应体中均不输出明文密码。

JWT 设计：

- 登录成功后返回 access token。
- 前端后续请求通过 `Authorization: Bearer <token>` 传递登录态。
- JWT payload 只包含 `userId`、`userAccount`、`userRole` 和过期时间，不放邮箱、手机号、画像字段或密码。
- 当前用户接口和资料更新接口解析 token 后，需要再次查询数据库，确认用户存在、未逻辑删除且未禁用。

配置项：

- 新增 `JWT_SECRET` 环境变量，用于 JWT 签名密钥。
- 新增 `spring.security.jwt.secret` 和 `spring.security.jwt.expiration` 配置。
- README 需要补充 `JWT_SECRET` 环境变量说明。

## 参数校验

注册和登录：

- `userAccount` 必填，长度 `4-32`，只允许字母、数字、下划线。
- `userPassword` 必填，长度 `8-64`。
- 注册时 `checkPassword` 必须与 `userPassword` 一致。

资料更新：

- `userName` 可选，最长 `64`。
- `userAvatar` 可选，最长 `512`。
- `targetPosition` 可选，最长 `128`。
- `techDirection` 可选，最长 `128`。
- `workYears` 范围为 `0-60`。
- `city` 可选，最长 `64`。
- `jobStatus` 可选，只允许空字符串、`looking`、`open`、`not_looking`。

## 错误处理

沿用现有 `BusinessException` 和 `GlobalExceptionHandler`，并扩展 `ErrorCode`：

- `NOT_LOGIN_ERROR(40100, "未登录")`
- `NO_AUTH_ERROR(40101, "无权限")`
- `FORBIDDEN_ERROR(40300, "禁止访问")`

错误场景：

- 参数不合法：返回 `PARAMS_ERROR`。
- 账号已存在：返回 `PARAMS_ERROR`，消息为 `账号已存在`。
- 登录账号或密码错误：返回 `PARAMS_ERROR`，消息为 `账号或密码错误`。
- token 缺失、过期或格式错误：返回 `NOT_LOGIN_ERROR`。
- 用户被禁用：返回 `FORBIDDEN_ERROR`。
- 未预期异常：继续返回 `SYSTEM_ERROR`。

## 测试设计

自动化测试不连接真实 MySQL、Redis 或 DeepSeek。

测试范围：

- `UserServiceImplTest`：注册成功、账号重复、两次密码不一致、登录成功、密码错误、禁用用户登录失败、更新资料成功。
- `JwtUtilsTest`：生成 token、解析 token、非法 token 识别。
- `UserControllerTest`：使用 MockMvc 覆盖注册、登录、获取当前用户、更新资料的基础响应结构。

Mock 策略：

- Service 层 mock `UserMapper`，避免数据库连接。
- Controller 层 mock `UserService`，只验证 HTTP 入参、鉴权头传递和统一响应形态。

## 交付文件

预计实现会新增或修改以下文件：

- `backend/src/main/java/com/mianshiba/ai/controller/UserController.java`
- `backend/src/main/java/com/mianshiba/ai/service/UserService.java`
- `backend/src/main/java/com/mianshiba/ai/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/mianshiba/ai/mapper/UserMapper.java`
- `backend/src/main/java/com/mianshiba/ai/model/entity/User.java`
- `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserRegisterRequest.java`
- `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserLoginRequest.java`
- `backend/src/main/java/com/mianshiba/ai/model/dto/user/UserUpdateProfileRequest.java`
- `backend/src/main/java/com/mianshiba/ai/model/vo/LoginUserVO.java`
- `backend/src/main/java/com/mianshiba/ai/model/vo/UserLoginVO.java`
- `backend/src/main/java/com/mianshiba/ai/config/SecurityConfig.java`
- `backend/src/main/java/com/mianshiba/ai/utils/JwtUtils.java`
- `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/sql/create_user_table.sql`
- `backend/src/test/java/com/mianshiba/ai/service/impl/UserServiceImplTest.java`
- `backend/src/test/java/com/mianshiba/ai/utils/JwtUtilsTest.java`
- `backend/src/test/java/com/mianshiba/ai/controller/UserControllerTest.java`
- `backend/README.md`

## 不在本次范围内

以下内容不属于首版用户模块：

- 邮箱验证码、手机号验证码、第三方登录。
- 刷新 token、Redis token 黑名单、单点登录。
- 完整 Spring Security 认证授权体系。
- 简历正文、技能标签、项目经历、教育经历等简历表。
- 推荐规则、推荐记录、投递记录、职位表。
- 管理端用户列表、封禁接口、角色权限管理。

## 成功标准

满足以下条件即认为用户模块首版完成：

- `user` 表结构明确，索引设计符合首版查询场景。
- 支持账号密码注册，账号唯一，密码以 BCrypt 哈希存储。
- 支持账号密码登录并返回 JWT。
- 支持携带 JWT 获取当前用户。
- 支持登录用户更新基础资料和轻量求职画像。
- API 响应均使用现有统一响应结构。
- 敏感字段不会返回给前端。
- 自动化测试覆盖核心成功路径和主要失败路径。
- 后端测试可在不连接真实 MySQL、Redis、DeepSeek 的情况下执行。

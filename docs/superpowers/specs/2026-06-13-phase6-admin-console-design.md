# Phase 6: 基础管理员后台设计

## 背景

Phase 1-5 已完成求职者侧主闭环：简历、职位、模拟面试、AI 复盘、投递管理、八股训练、错题本、知识点掌握度和求职作战台。当前系统已经具备完整的用户侧训练和求职管理能力，但缺少平台管理员后台。

现有用户模型已经包含 `userRole` 和 `userStatus` 字段，JWT 中也包含 `userRole` claim。Phase 6 不需要重新设计账号体系，而是在现有用户体系上补齐管理员鉴权、管理员页面、用户管理和平台运营总览。

## 目标

- 建立管理员接口鉴权边界。
- 提供管理员端入口、路由守卫和专用布局。
- 支持管理员查看平台运营核心数据。
- 支持管理员查看、搜索、禁用、启用用户。
- 支持管理员查看用户详情和用户使用数据概览。
- 支持管理员调整用户角色，但防止管理员将自己降级或禁用。

## 非目标

- 不做复杂 RBAC 权限系统。
- 不新增管理员账号表。
- 不做管理员操作审计。
- 不做 AI 调用日志治理。
- 不做 Prompt 模板管理。
- 不做内容审核后台。
- 不做支付、套餐、额度或商业化后台。
- 不做复杂运营图表或 BI 系统。

## 权限模型

复用 `user.user_role`：

- `user`: 普通用户。
- `admin`: 管理员。

复用 `user.user_status`：

- `0`: 正常。
- `1`: 禁用。

管理员接口鉴权规则：

1. 请求必须携带合法 `Authorization: Bearer <token>`。
2. JWT 必须能解析出用户 ID。
3. 用户必须存在且未逻辑删除。
4. 用户状态必须为正常。
5. 用户角色必须为 `admin`。

不满足登录条件返回 `NOT_LOGIN_ERROR`。用户被禁用返回 `FORBIDDEN_ERROR`。用户已登录但不是管理员返回 `NO_AUTH_ERROR`。

## 安全约束

- 管理员不能禁用自己。
- 管理员不能将自己的角色从 `admin` 改为 `user`。
- 用户角色只能是 `user` 或 `admin`。
- 用户状态只能是正常或禁用。
- 所有管理员接口都必须经过管理员鉴权。
- 注册用户默认角色仍为 `user`。

管理员账号来源：

- 本期不做管理员注册入口。
- 管理员账号可通过数据库初始化或手动修改 `user_role` 创建。

## 后端设计

### 模块结构

新增标准分层：

- `AdminController`: `/api/admin` 前缀。
- `AdminService`: 管理员后台业务接口。
- `AdminServiceImpl`: 管理员鉴权、平台总览、用户管理实现。
- DTO：用户查询条件、角色更新请求。
- VO：平台总览、用户列表项、用户详情。

### API 设计

#### 平台总览

`GET /api/admin/overview`

返回字段：

- `totalUsers`: 用户总数。
- `enabledUsers`: 正常用户数。
- `disabledUsers`: 禁用用户数。
- `adminUsers`: 管理员数量。
- `resumeCount`: 简历数量。
- `interviewCount`: 面试会话数量。
- `completedInterviewCount`: 已完成面试数量。
- `applicationCount`: 投递记录数量。
- `trainingPlanCount`: 八股训练计划数量。
- `trainingAnswerCount`: 八股作答数量。
- `trainingReviewCount`: AI 批改数量。

#### 用户列表

`GET /api/admin/users`

查询参数：

- `keyword`: 搜索账号、昵称、邮箱。
- `userStatus`: 用户状态，0 正常、1 禁用。
- `userRole`: 角色，`user` / `admin`。

返回列表项：

- 用户 ID。
- 账号。
- 昵称。
- 头像。
- 角色。
- 状态。
- 邮箱。
- 目标岗位。
- 技术方向。
- 城市。
- 注册时间。

#### 用户详情

`GET /api/admin/users/{id}`

返回字段：

- 用户基础信息。
- 简历数量。
- 面试数量。
- 已完成面试数量。
- 投递数量。
- 训练计划数量。
- 八股作答数量。
- AI 批改数量。

#### 禁用用户

`PUT /api/admin/users/{id}/disable`

规则：

- 目标用户必须存在。
- 不能禁用当前管理员自己。
- 设置 `userStatus = 1`。

#### 启用用户

`PUT /api/admin/users/{id}/enable`

规则：

- 目标用户必须存在。
- 设置 `userStatus = 0`。

#### 更新用户角色

`PUT /api/admin/users/{id}/role`

请求体：

```json
{
  "userRole": "admin"
}
```

规则：

- 目标用户必须存在。
- `userRole` 只能是 `user` 或 `admin`。
- 当前管理员不能将自己降级为 `user`。

### 统计来源

平台总览和用户详情直接复用现有表统计：

- `user`
- `resume`
- `interview_session`
- `job_application`
- `training_plan`
- `training_answer`
- `training_answer_review`

不新增统计缓存表。管理员后台初期以实时查询为主，保持实现简单。

## 前端设计

### 路由

新增管理员路由：

- `/admin`: 管理员总览。
- `/admin/users`: 用户管理。
- `/admin/users/:id`: 用户详情。

路由 meta：

- `requiresAuth: true`
- `requiresAdmin: true`

### 路由守卫

扩展现有路由守卫：

- 未登录访问管理员路由，跳转 `/login`。
- 已登录但 `userInfo.userRole !== 'admin'`，跳转首页。

本期不新增单独无权限页。

### 管理员布局

新增 `AdminLayout.vue`：

- 顶部保留品牌和管理员身份展示。
- 左侧提供管理员导航。
- 导航项：总览、用户管理、返回用户端。
- 视觉风格沿用现有 Neubrutalism 设计系统。

### 用户端入口

修改 `MainLayout.vue`：

- 仅当当前用户 `userRole === 'admin'` 时展示“管理后台”入口。

### 页面设计

#### `AdminDashboardPage.vue`

展示：

- 用户总数。
- 正常用户数。
- 禁用用户数。
- 管理员数量。
- 简历数量。
- 面试数量。
- 投递数量。
- 训练计划数量。
- 八股作答数量。
- AI 批改数量。

#### `AdminUserListPage.vue`

展示：

- 搜索框。
- 状态筛选。
- 角色筛选。
- 用户表格。
- 禁用/启用按钮。
- 查看详情按钮。

#### `AdminUserDetailPage.vue`

展示：

- 用户基础信息。
- 用户使用数据概览。
- 角色选择。
- 禁用/启用操作。
- 返回用户列表。

## 前端数据层

新增：

- `frontend/src/types/admin.ts`
- `frontend/src/api/admin.ts`
- `frontend/src/stores/admin.ts`

Store 状态：

- `overview`
- `users`
- `currentUser`
- `loading`

Store actions：

- `fetchOverview`
- `fetchUsers`
- `fetchUser`
- `disableUser`
- `enableUser`
- `updateUserRole`

## 错误处理

- 非管理员访问管理员接口返回 `NO_AUTH_ERROR`。
- 被禁用用户访问返回 `FORBIDDEN_ERROR`。
- 目标用户不存在返回 `NOT_FOUND_ERROR`。
- 禁用自己或降级自己返回 `PARAMS_ERROR` 或 `FORBIDDEN_ERROR`。
- 前端接口失败时展示 Element Plus 错误提示。

## 测试策略

后端测试：

- 普通用户访问管理员接口被拒绝。
- 管理员可以获取平台总览。
- 管理员可以查询用户列表。
- 管理员可以禁用其他用户。
- 管理员不能禁用自己。
- 管理员可以启用用户。
- 管理员可以修改其他用户角色。
- 管理员不能将自己降级为普通用户。

前端验证：

- `npm run type-check`。
- `npm run build-only`。
- 普通用户访问 `/admin` 被拦截。
- 管理员可看到管理后台入口。
- 管理员用户列表、详情页、总览页可加载空态和有数据态。

## 分期建议

Phase 6 可以拆为 6 个实现任务：

1. 后端管理员 DTO/VO/Service/Controller 基础结构。
2. 后端管理员鉴权、平台总览、用户列表和详情。
3. 后端用户禁用/启用/角色更新和测试。
4. 前端管理员类型、API、Store、路由守卫和 AdminLayout。
5. 前端管理员总览和用户列表页面。
6. 前端用户详情页面与全量验证。

## 成功标准

- 管理员可以访问 `/admin` 管理后台。
- 普通用户无法访问管理员路由和管理员 API。
- 管理员可以查看平台核心数据。
- 管理员可以搜索、查看、禁用、启用用户。
- 管理员可以调整其他用户角色。
- 管理员不能禁用自己，也不能把自己降级为普通用户。

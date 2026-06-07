# 前端用户模块设计文档

> 项目：面试吧 — AI 面试模拟平台
> 日期：2026-05-17
> 风格：Neubrutalism（新粗野主义）

## 1. 视觉设计系统

### 1.1 风格定义

Neubrutalism：粗黑边框 + 硬偏移阴影 + 高饱和撞色 + 微圆角。视觉辨识度极高，面向年轻求职者群体。

### 1.2 核心视觉变量

| 变量 | 值 | 说明 |
|------|-----|------|
| 边框 | `2-3px solid #2D3436` | 所有卡片、按钮、输入框统一 |
| 阴影 | `4px 4px 0 #2D3436` | 硬偏移阴影，hover 时增至 `6px 6px 0` |
| 圆角 | `4-8px` | 微圆角，不过度圆润 |
| 过渡 | `150-200ms ease` | 阴影和位移变化 |
| 按钮按压 | `transform: translate(4px, 4px); box-shadow: none` | 点击时阴影消失 + 下移，模拟物理按压 |

### 1.3 配色方案

| 角色 | 色值 | 用途 |
|------|------|------|
| `--color-bg` | `#FEF9EF` | 页面底色（暖奶白） |
| `--color-primary` | `#6C5CE7` | 品牌色、主按钮 |
| `--color-secondary` | `#00CEC9` | 辅助信息、标签、链接 |
| `--color-accent` | `#FD79A8` | CTA、错误提示、重要标记 |
| `--color-success` | `#00B894` | 成功状态、保存按钮 |
| `--color-warning` | `#FDCB6E` | 警告提示 |
| `--color-text` | `#2D3436` | 正文文字、边框色 |
| `--color-card` | `#FFFFFF` | 卡片/表单背景 |
| `--color-muted` | `#636E72` | 次要文字 |

### 1.4 字体

| 用途 | 字体 | 备选 |
|------|------|------|
| 标题 | Lexend Mega | Outfit |
| 正文 | Public Sans | Work Sans |

Google Fonts 引入：
```
@import url('https://fonts.googleapis.com/css2?family=Lexend+Mega:wght@400;500;600;700&family=Public+Sans:wght@300;400;500;600;700&display=swap');
```

### 1.5 Element Plus 主题定制

通过 CSS 变量覆盖 Element Plus 默认主题，使组件库融入 Neubrutalism 风格：
- `--el-border-radius-base: 6px`
- `--el-border-width: 2px`
- `--el-color-primary: #6C5CE7`
- 输入框增加黑边框和偏移阴影覆盖

## 2. 页面设计

### 2.1 登录/注册页（`/login`）

**布局：左右分屏**

**左侧 — 品牌展示区（60%宽度）**
- 暖白背景 + 大面积紫罗兰色块装饰（粗边框 + 偏移阴影）
- 品牌名"面试吧"：Lexend Mega 超大字体，黑边框描边效果
- Slogan："AI 模拟面试，拿下你的 dream offer"
- 3 个特性卡片（SVG 图标，不用 emoji）：
  - 智能题库 — AI 实时生成面试问题
  - 模拟对话 — 沉浸式面试体验
  - 即时反馈 — 面试表现全分析
- 每个卡片：粗边框 + 偏移阴影，hover 阴影增大

**右侧 — 表单区（40%宽度）**
- 白色卡片，3px 黑边框 + 4px 偏移阴影
- 顶部 Tab 切换登录/注册（当前 Tab 底部粗色条）
- 输入框：白底 + 2px 黑边框，focus 时出现颜色条
- 主按钮：紫罗兰 `#6C5CE7` + 黑边框 + 偏移阴影
- hover：阴影增大 + 上移；active：阴影消失 + 下移（按压感）

**登录表单字段**
- 账号（userAccount）：4-32 位字母/数字/下划线
- 密码（userPassword）：8-64 位

**注册表单字段**
- 账号（userAccount）：4-32 位字母/数字/下划线
- 密码（userPassword）：8-64 位
- 确认密码（checkPassword）：8-64 位，需与密码一致

### 2.2 首页（`/`）

- 顶部导航栏：白底 + 底部粗黑边框线
  - 左：品牌 Logo + 名称
  - 右：用户头像 + 昵称下拉菜单（个人资料、退出登录）
- 主体区域：
  - 欢迎卡片（大标题 + 用户昵称），粗边框 + 偏移阴影
  - 功能入口骨架（面试模拟、历史记录等占位卡片）
  - 统计数据区域骨架

### 2.3 用户资料编辑页（`/profile`）

- 使用 MainLayout
- 两栏布局：左侧头像区，右侧表单
- 表单字段（对应后端 `UserUpdateProfileRequest`）：
  - 昵称（userName）：最长 64 字符
  - 目标岗位（targetPosition）：最长 128 字符
  - 技术方向（techDirection）：最长 128 字符
  - 工作年限（workYears）：0-60
  - 城市（city）：最长 64 字符
  - 求职状态（jobStatus）：Select 下拉 — 不限 / 正在看机会(looking) / 开放机会(open) / 暂不考虑(not_looking)
- 保存按钮：翠绿色 `#00B894` + 黑边框 + 偏移阴影

## 3. 技术架构

### 3.1 目录结构

```
src/
├── api/
│   └── user.ts              # 用户相关 API
├── assets/
│   └── styles/
│       └── variables.css     # CSS 变量（配色、阴影、边框、字体）
├── components/
│   ├── NbCard.vue            # Neubrutalism 卡片
│   ├── NbButton.vue          # Neubrutalism 按钮
│   ├── NbInput.vue           # Neubrutalism 输入框
│   └── icons/                # SVG 图标组件
│       ├── IconTarget.vue
│       ├── IconChat.vue
│       └── IconChart.vue
├── layouts/
│   ├── AuthLayout.vue        # 登录/注册左右分屏布局
│   └── MainLayout.vue        # 主应用布局（顶栏 + 内容区）
├── router/
│   └── index.ts              # 路由配置 + 导航守卫
├── stores/
│   └── user.ts               # 用户状态（Pinia Setup Store）
├── types/
│   └── user.ts               # TypeScript 类型定义
├── utils/
│   └── request.ts            # axios 实例 + 拦截器
└── views/
    ├── auth/
    │   └── LoginPage.vue     # 登录/注册页
    ├── home/
    │   └── HomePage.vue      # 首页
    └── profile/
        └── ProfilePage.vue   # 用户资料编辑
```

### 3.2 依赖安装

新增生产依赖：
- `axios` — HTTP 请求
- `element-plus` — UI 组件库
- `@element-plus/icons-vue` — Element Plus 图标

### 3.3 TypeScript 类型定义

```typescript
// types/user.ts

interface LoginUserVO {
  id: number
  userAccount: string
  userName: string
  userAvatar: string
  userRole: string
  userStatus: number
  email: string
  phone: string
  targetPosition: string
  techDirection: string
  workYears: number
  city: string
  jobStatus: string
  createTime: string
}

interface UserLoginVO {
  token: string
  user: LoginUserVO
}

interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

interface LoginRequest {
  userAccount: string
  userPassword: string
}

interface RegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

interface UpdateProfileRequest {
  userName?: string
  userAvatar?: string
  targetPosition?: string
  techDirection?: string
  workYears?: number
  city?: string
  jobStatus?: string
}
```

### 3.4 API 层（`src/api/user.ts`）

| 方法 | HTTP | 路径 | 参数 | 返回 |
|------|------|------|------|------|
| `login` | POST | `/api/user/login` | `LoginRequest` | `BaseResponse<UserLoginVO>` |
| `register` | POST | `/api/user/register` | `RegisterRequest` | `BaseResponse<number>` |
| `getCurrentUser` | GET | `/api/user/current` | — | `BaseResponse<LoginUserVO>` |
| `updateProfile` | PUT | `/api/user/profile` | `UpdateProfileRequest` | `BaseResponse<LoginUserVO>` |

### 3.5 状态管理（`src/stores/user.ts`）

Pinia Setup Store，管理：
- `token: string | null` — 从 localStorage 初始化
- `userInfo: LoginUserVO | null`
- `isLoggedIn` — computed
- `login(data)` — 存储 token + userInfo
- `logout()` — 清除 token + userInfo，跳转 `/login`
- `fetchCurrentUser()` — 调用 API 刷新用户信息
- `updateProfile(data)` — 调用 API 更新资料

Token 持久化：localStorage key `mianshiba_token`

### 3.6 路由设计（`src/router/index.ts`）

| 路径 | 组件 | 布局 | 需登录 |
|------|------|------|--------|
| `/login` | LoginPage | AuthLayout | 否 |
| `/` | HomePage | MainLayout | 是 |
| `/profile` | ProfilePage | MainLayout | 是 |

导航守卫：
- 未登录访问需登录页面 → 重定向 `/login`
- 已登录访问 `/login` → 重定向 `/`

### 3.7 HTTP 拦截器（`src/utils/request.ts`）

- **请求拦截器**：从 localStorage 读取 token，附加 `Authorization: Bearer <token>`
- **响应拦截器**：
  - 统一解包 `BaseResponse`，`code !== 0` 时弹出 Element Plus `ElMessage.error`
  - `code === 40100`（未登录）时清除 token + 跳转 `/login`

### 3.8 环境变量

```
# .env.development
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=
```

## 4. 认证流程

1. 用户提交登录表单 → 调用 `POST /api/user/login`
2. 成功 → token 存入 localStorage，userInfo 存入 Pinia store
3. 后续请求 → axios 拦截器自动附加 `Authorization: Bearer <token>`
4. 页面刷新 → 路由守卫检查 localStorage token，有则调用 `GET /api/user/current` 恢复用户信息
5. token 过期/无效 → 响应拦截器捕获 40100，清除 token，跳转登录页
6. 退出登录 → 清除 localStorage + Pinia store，跳转 `/login`

## 5. 通用组件设计

### NbButton
- Props: `type`（primary/secondary/accent/success）, `loading`, `disabled`, `size`
- 样式：对应颜色背景 + 黑边框 + 偏移阴影
- hover：阴影增大 + translate(-1px, -1px)
- active：阴影消失 + translate(4px, 4px)

### NbCard
- Props: `hoverable`
- 样式：白底 + 黑边框 + 偏移阴影
- hoverable 时：hover 阴影增大 + translate(-2px, -2px)

### NbInput
- Props: `modelValue`, `label`, `error`, `placeholder`, `type`
- 样式：白底 + 黑边框，focus 时边框变为主色
- error 状态：边框变为 accent 色 + 错误文字

## 6. 不做的事情（YAGNI）

- 不做记住密码功能
- 不做忘记密码功能（后端未实现）
- 不做第三方登录
- 不做 token 自动刷新
- 不做国际化
- 不做暗色模式切换
- 不做头像上传（后端 `userAvatar` 为 URL 字符串，暂用默认头像）

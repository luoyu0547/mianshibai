# 前端用户模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建前端用户模块，包括 Neubrutalism 风格登录/注册页、用户资料编辑页、首页骨架，实现完整的 JWT 认证流程。

**Architecture：** Vue 3 + TypeScript + Element Plus（Neubrutalism 风格定制）+ Pinia + Vue Router，分层架构（utils → types → api → stores → components → layouts → views）。

**Tech Stack：** Vue 3.5, TypeScript 5.9, Vite 7, Element Plus, Pinia 3, Vue Router 4, axios

---

## 文件结构总览

```
frontend/src/
├── api/
│   └── user.ts
├── assets/
│   └── styles/
│       └── variables.css
├── components/
│   ├── NbButton.vue
│   ├── NbCard.vue
│   ├── NbInput.vue
│   └── icons/
│       ├── IconChat.vue
│       ├── IconChart.vue
│       └── IconTarget.vue
├── layouts/
│   ├── AuthLayout.vue
│   └── MainLayout.vue
├── router/
│   └── index.ts
├── stores/
│   └── user.ts
├── types/
│   └── user.ts
├── utils/
│   └── request.ts
├── views/
│   ├── auth/
│   │   └── LoginPage.vue
│   ├── home/
│   │   └── HomePage.vue
│   └── profile/
│       └── ProfilePage.vue
├── App.vue
└── main.ts
```

---

## Task 1: 安装依赖

**Files:**
- Modify: `frontend/package.json`

- [ ] **Step 1: 在 frontend 目录下安装依赖**

```bash
cd D:\code_schl\mianshiba\frontend
npm install axios element-plus @element-plus/icons-vue
```

- [ ] **Step 2: 验证安装**

Run: `npm ls axios element-plus @element-plus/icons-vue`
Expected: 三个包都显示版本号，无报错

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json
# 如果有 node_modules 未 gitignore，确保不提交
git commit -m "deps: add axios, element-plus, @element-plus/icons-vue"
```

---

## Task 2: 创建 CSS 变量与 Element Plus 定制样式

**Files:**
- Create: `frontend/src/assets/styles/variables.css`

- [ ] **Step 1: 创建 CSS 变量文件**

```css
/* src/assets/styles/variables.css */
@import url('https://fonts.googleapis.com/css2?family=Lexend+Mega:wght@400;500;600;700&family=Public+Sans:wght@300;400;500;600;700&display=swap');

:root {
  /* Neubrutalism 配色 */
  --nb-bg: #FEF9EF;
  --nb-card: #FFFFFF;
  --nb-text: #2D3436;
  --nb-muted: #636E72;
  --nb-primary: #6C5CE7;
  --nb-secondary: #00CEC9;
  --nb-accent: #FD79A8;
  --nb-success: #00B894;
  --nb-warning: #FDCB6E;
  --nb-border: #2D3436;

  /* 阴影与边框 */
  --nb-border-width: 2px;
  --nb-border: var(--nb-border-width) solid var(--nb-border);
  --nb-shadow: 4px 4px 0 var(--nb-border);
  --nb-shadow-hover: 6px 6px 0 var(--nb-border);
  --nb-radius: 6px;
  --nb-radius-lg: 8px;

  /* 字体 */
  --font-heading: 'Lexend Mega', 'Outfit', sans-serif;
  --font-body: 'Public Sans', 'Work Sans', sans-serif;

  /* 过渡 */
  --nb-transition: 150ms ease;

  /* Element Plus 覆盖 */
  --el-color-primary: var(--nb-primary);
  --el-color-success: var(--nb-success);
  --el-color-warning: var(--nb-warning);
  --el-color-danger: var(--nb-accent);
  --el-border-radius-base: var(--nb-radius);
  --el-border-width: var(--nb-border-width);
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-body);
  background-color: var(--nb-bg);
  color: var(--nb-text);
}

/* 覆盖 Element Plus 输入框，融入 Neubrutalism */
.el-input__wrapper {
  box-shadow: var(--nb-shadow) !important;
  border: var(--nb-border) !important;
  border-radius: var(--nb-radius) !important;
  background: var(--nb-card) !important;
  padding: 4px 11px !important;
  transition: var(--nb-transition);
}

.el-input__wrapper:hover {
  box-shadow: var(--nb-shadow-hover) !important;
  transform: translate(-1px, -1px);
}

.el-input__wrapper.is-focus {
  box-shadow: var(--nb-shadow-hover) !important;
  border-color: var(--nb-primary) !important;
}

/* 覆盖 Element Plus 按钮 */
.el-button {
  font-family: var(--font-heading);
  font-weight: 600;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius);
  transition: var(--nb-transition);
}

.el-button:hover {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-1px, -1px);
}

.el-button:active {
  box-shadow: none;
  transform: translate(4px, 4px);
}

/* 覆盖 Element Plus Select */
.el-select .el-input__wrapper {
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
}

.el-select-dropdown {
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius);
}

/* 覆盖 tabs */
.el-tabs__item {
  font-family: var(--font-heading);
  font-weight: 600;
}
```

- [ ] **Step 2: Commit**

```bash
git add src/assets/styles/variables.css
git commit -m "style: add Neubrutalism CSS variables and Element Plus overrides"
```

---

## Task 3: 创建 TypeScript 类型定义

**Files:**
- Create: `frontend/src/types/user.ts`

- [ ] **Step 1: 创建用户相关类型**

```typescript
// src/types/user.ts

export interface LoginUserVO {
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

export interface UserLoginVO {
  token: string
  user: LoginUserVO
}

export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export interface LoginRequest {
  userAccount: string
  userPassword: string
}

export interface RegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

export interface UpdateProfileRequest {
  userName?: string
  userAvatar?: string
  targetPosition?: string
  techDirection?: string
  workYears?: number
  city?: string
  jobStatus?: string
}
```

- [ ] **Step 2: Commit**

```bash
git add src/types/user.ts
git commit -m "types: add user-related TypeScript interfaces"
```

---

## Task 4: 创建 axios 请求工具

**Files:**
- Create: `frontend/src/utils/request.ts`

- [ ] **Step 1: 创建 axios 实例**

```typescript
// src/utils/request.ts
import axios, { type AxiosError, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

// 请求拦截器：附加 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('mianshiba_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：解包 + 错误处理
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      // 40100 未登录
      if (data.code === 40100) {
        localStorage.removeItem('mianshiba_token')
        router.push('/login')
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error: AxiosError) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default request
```

- [ ] **Step 2: Commit**

```bash
git add src/utils/request.ts
git commit -m "utils: add axios instance with interceptors"
```

---

## Task 5: 创建用户 API 层

**Files:**
- Create: `frontend/src/api/user.ts`

- [ ] **Step 1: 创建用户 API 方法**

```typescript
// src/api/user.ts
import request from '@/utils/request'
import type {
  BaseResponse,
  LoginRequest,
  RegisterRequest,
  UserLoginVO,
  LoginUserVO,
  UpdateProfileRequest,
} from '@/types/user'

export function login(data: LoginRequest) {
  return request.post<BaseResponse<UserLoginVO>>('/api/user/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<BaseResponse<number>>('/api/user/register', data)
}

export function getCurrentUser() {
  return request.get<BaseResponse<LoginUserVO>>('/api/user/current')
}

export function updateProfile(data: UpdateProfileRequest) {
  return request.put<BaseResponse<LoginUserVO>>('/api/user/profile', data)
}
```

- [ ] **Step 2: Commit**

```bash
git add src/api/user.ts
git commit -m "api: add user authentication and profile APIs"
```

---

## Task 6: 创建 Pinia 用户 Store

**Files:**
- Create: `frontend/src/stores/user.ts`

- [ ] **Step 1: 创建用户状态管理**

```typescript
// src/stores/user.ts
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, getCurrentUser, updateProfile as updateProfileApi } from '@/api/user'
import type { LoginUserVO, LoginRequest, UpdateProfileRequest } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(localStorage.getItem('mianshiba_token'))
  const userInfo = ref<LoginUserVO | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  // Actions
  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    if (res.data.code === 0) {
      const { token: newToken, user } = res.data.data
      token.value = newToken
      userInfo.value = user
      localStorage.setItem('mianshiba_token', newToken)
      return true
    }
    return false
  }

  async function fetchCurrentUser() {
    const res = await getCurrentUser()
    if (res.data.code === 0) {
      userInfo.value = res.data.data
      return true
    }
    return false
  }

  async function updateProfile(data: UpdateProfileRequest) {
    const res = await updateProfileApi(data)
    if (res.data.code === 0) {
      userInfo.value = res.data.data
      return true
    }
    return false
  }

  function logout() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('mianshiba_token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    fetchCurrentUser,
    updateProfile,
    logout,
  }
})
```

- [ ] **Step 2: Commit**

```bash
git add src/stores/user.ts
git commit -m "store: add Pinia user store with auth state"
```

---

## Task 7: 创建路由配置（含导航守卫）

**Files:**
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: 重写路由配置**

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginPage.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/home/HomePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/profile/ProfilePage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 导航守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 如果本地有 token 但 store 中没有用户信息，尝试恢复
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchCurrentUser()
    } catch {
      // 恢复失败（token 过期），清除 token
      userStore.logout()
    }
  }

  // 未登录访问需认证页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  // 已登录访问登录页，跳转到首页
  if (to.meta.public && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})

export default router
```

- [ ] **Step 2: Commit**

```bash
git add src/router/index.ts
git commit -m "router: add routes, guards, and auth redirection logic"
```

---

## Task 8: 创建 SVG 图标组件

**Files:**
- Create: `frontend/src/components/icons/IconTarget.vue`
- Create: `frontend/src/components/icons/IconChat.vue`
- Create: `frontend/src/components/icons/IconChart.vue`

- [ ] **Step 1: 创建三个 SVG 图标**

```vue
<!-- src/components/icons/IconTarget.vue -->
<template>
  <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="12" r="10"/>
    <circle cx="12" cy="12" r="6"/>
    <circle cx="12" cy="12" r="2"/>
  </svg>
</template>
```

```vue
<!-- src/components/icons/IconChat.vue -->
<template>
  <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
  </svg>
</template>
```

```vue
<!-- src/components/icons/IconChart.vue -->
<template>
  <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <line x1="18" y1="20" x2="18" y2="10"/>
    <line x1="12" y1="20" x2="12" y2="4"/>
    <line x1="6" y1="20" x2="6" y2="14"/>
  </svg>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add src/components/icons/
git commit -m "components: add SVG icon components for auth page features"
```

---

## Task 9: 创建 Neubrutalism 通用组件

**Files:**
- Create: `frontend/src/components/NbCard.vue`
- Create: `frontend/src/components/NbButton.vue`

- [ ] **Step 1: 创建 NbCard 组件**

```vue
<!-- src/components/NbCard.vue -->
<template>
  <div :class="['nb-card', { 'nb-card--hoverable': hoverable }]">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  hoverable?: boolean
}

defineProps<Props>()
</script>

<style scoped>
.nb-card {
  background: var(--nb-card);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius-lg);
  padding: 24px;
  transition: var(--nb-transition);
}

.nb-card--hoverable:hover {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-2px, -2px);
}
</style>
```

- [ ] **Step 2: 创建 NbButton 组件**

```vue
<!-- src/components/NbButton.vue -->
<template>
  <button
    :class="['nb-button', `nb-button--${type}`, { 'nb-button--loading': loading, 'nb-button--block': block }]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="nb-button__spinner"></span>
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  type?: 'primary' | 'secondary' | 'accent' | 'success'
  loading?: boolean
  disabled?: boolean
  block?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'primary',
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

function handleClick(event: MouseEvent) {
  if (!props.loading && !props.disabled) {
    emit('click', event)
  }
}
</script>

<style scoped>
.nb-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 15px;
  padding: 12px 28px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  box-shadow: var(--nb-shadow);
  cursor: pointer;
  transition: var(--nb-transition);
  background: var(--nb-card);
  color: var(--nb-text);
}

.nb-button:hover:not(:disabled) {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-1px, -1px);
}

.nb-button:active:not(:disabled) {
  box-shadow: none;
  transform: translate(4px, 4px);
}

.nb-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nb-button--primary {
  background: var(--nb-primary);
  color: #fff;
}

.nb-button--secondary {
  background: var(--nb-secondary);
  color: var(--nb-text);
}

.nb-button--accent {
  background: var(--nb-accent);
  color: #fff;
}

.nb-button--success {
  background: var(--nb-success);
  color: #fff;
}

.nb-button--block {
  width: 100%;
}

.nb-button__spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: nb-spin 0.8s linear infinite;
}

@keyframes nb-spin {
  to { transform: rotate(360deg); }
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add src/components/NbCard.vue src/components/NbButton.vue
git commit -m "components: add Neubrutalism Card and Button components"
```

---

## Task 10: 创建布局组件

**Files:**
- Create: `frontend/src/layouts/AuthLayout.vue`
- Create: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: 创建 AuthLayout（登录/注册左右分屏）**

```vue
<!-- src/layouts/AuthLayout.vue -->
<template>
  <div class="auth-layout">
    <!-- 左侧品牌区 -->
    <div class="auth-layout__left">
      <div class="auth-layout__decorator auth-layout__decorator--1"></div>
      <div class="auth-layout__decorator auth-layout__decorator--2"></div>

      <div class="auth-layout__brand">
        <h1 class="auth-layout__title">面试吧</h1>
        <p class="auth-layout__slogan">AI 模拟面试，拿下你的 dream offer</p>
      </div>

      <div class="auth-layout__features">
        <NbCard hoverable class="feature-card">
          <div class="feature-card__icon"><IconTarget /></div>
          <div class="feature-card__title">智能题库</div>
          <div class="feature-card__desc">AI 实时生成面试问题</div>
        </NbCard>

        <NbCard hoverable class="feature-card">
          <div class="feature-card__icon"><IconChat /></div>
          <div class="feature-card__title">模拟对话</div>
          <div class="feature-card__desc">沉浸式面试体验</div>
        </NbCard>

        <NbCard hoverable class="feature-card">
          <div class="feature-card__icon"><IconChart /></div>
          <div class="feature-card__title">即时反馈</div>
          <div class="feature-card__desc">面试表现全分析</div>
        </NbCard>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="auth-layout__right">
      <div class="auth-layout__form-wrapper">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import NbCard from '@/components/NbCard.vue'
import IconTarget from '@/components/icons/IconTarget.vue'
import IconChat from '@/components/icons/IconChat.vue'
import IconChart from '@/components/icons/IconChart.vue'
</script>

<style scoped>
.auth-layout {
  display: flex;
  min-height: 100vh;
  background: var(--nb-bg);
}

.auth-layout__left {
  flex: 0 0 60%;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px;
  overflow: hidden;
}

.auth-layout__decorator {
  position: absolute;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius-lg);
}

.auth-layout__decorator--1 {
  top: 40px;
  left: 40px;
  width: 120px;
  height: 120px;
  background: var(--nb-primary);
  transform: rotate(-12deg);
}

.auth-layout__decorator--2 {
  bottom: 60px;
  right: 60px;
  width: 80px;
  height: 80px;
  background: var(--nb-secondary);
  transform: rotate(15deg);
}

.auth-layout__brand {
  text-align: center;
  margin-bottom: 48px;
  z-index: 1;
}

.auth-layout__title {
  font-family: var(--font-heading);
  font-size: 64px;
  font-weight: 700;
  color: var(--nb-text);
  margin: 0 0 16px;
  text-shadow: 3px 3px 0 var(--nb-primary);
  -webkit-text-stroke: 2px var(--nb-border);
}

.auth-layout__slogan {
  font-family: var(--font-body);
  font-size: 20px;
  color: var(--nb-muted);
  margin: 0;
}

.auth-layout__features {
  display: flex;
  gap: 24px;
  z-index: 1;
}

.feature-card {
  width: 200px;
  text-align: center;
}

.feature-card__icon {
  color: var(--nb-primary);
  margin-bottom: 12px;
}

.feature-card__title {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.feature-card__desc {
  font-size: 14px;
  color: var(--nb-muted);
}

.auth-layout__right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--nb-bg);
}

.auth-layout__form-wrapper {
  width: 100%;
  max-width: 420px;
}

/* 移动端响应式 */
@media (max-width: 1024px) {
  .auth-layout {
    flex-direction: column;
  }

  .auth-layout__left {
    flex: none;
    padding: 32px 24px;
    min-height: auto;
  }

  .auth-layout__title {
    font-size: 48px;
  }

  .auth-layout__features {
    flex-direction: column;
    align-items: center;
  }

  .feature-card {
    width: 100%;
    max-width: 300px;
  }

  .auth-layout__right {
    padding: 32px 24px;
  }
}
</style>
```

- [ ] **Step 2: 创建 MainLayout**

```vue
<!-- src/layouts/MainLayout.vue -->
<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <header class="main-layout__header">
      <div class="main-layout__header-inner">
        <router-link to="/" class="main-layout__brand">
          <span class="main-layout__brand-text">面试吧</span>
        </router-link>

        <nav class="main-layout__nav">
          <router-link to="/" class="main-layout__nav-link">首页</router-link>
          <router-link to="/profile" class="main-layout__nav-link">个人资料</router-link>
        </nav>

        <div class="main-layout__user">
          <el-dropdown v-if="userStore.isLoggedIn" trigger="click">
            <div class="main-layout__user-trigger">
              <div class="main-layout__avatar">
                {{ userStore.userInfo?.userName?.[0] || userStore.userInfo?.userAccount?.[0] || 'U' }}
              </div>
              <span class="main-layout__username">
                {{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || '用户' }}
              </span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">个人资料</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <router-link v-else to="/login" class="main-layout__login-link">
            <NbButton type="primary">登录</NbButton>
          </router-link>
        </div>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="main-layout__main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import NbButton from '@/components/NbButton.vue'

const userStore = useUserStore()
const router = useRouter()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: var(--nb-bg);
}

.main-layout__header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  box-shadow: 0 2px 0 rgba(0, 0, 0, 0.05);
}

.main-layout__header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.main-layout__brand {
  text-decoration: none;
}

.main-layout__brand-text {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--nb-text);
  text-shadow: 2px 2px 0 var(--nb-primary);
}

.main-layout__nav {
  display: flex;
  gap: 24px;
}

.main-layout__nav-link {
  font-family: var(--font-body);
  font-weight: 500;
  font-size: 15px;
  color: var(--nb-muted);
  text-decoration: none;
  padding: 8px 4px;
  border-bottom: 3px solid transparent;
  transition: var(--nb-transition);
}

.main-layout__nav-link:hover,
.main-layout__nav-link.router-link-active {
  color: var(--nb-text);
  border-bottom-color: var(--nb-primary);
}

.main-layout__user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--nb-radius);
  transition: var(--nb-transition);
}

.main-layout__user-trigger:hover {
  background: rgba(108, 92, 231, 0.08);
}

.main-layout__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--nb-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
  border: 2px solid var(--nb-border);
}

.main-layout__username {
  font-weight: 500;
  color: var(--nb-text);
}

.main-layout__main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

.main-layout__login-link {
  text-decoration: none;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add src/layouts/
git commit -m "layouts: add AuthLayout (split-screen) and MainLayout (navbar)"
```

---

## Task 11: 创建登录/注册页

**Files:**
- Create: `frontend/src/views/auth/LoginPage.vue`

- [ ] **Step 1: 创建登录/注册页**

```vue
<!-- src/views/auth/LoginPage.vue -->
<template>
  <AuthLayout>
    <NbCard class="login-card">
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 登录 Tab -->
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            class="login-form"
          >
            <el-form-item label="账号" prop="userAccount">
              <el-input
                v-model="loginForm.userAccount"
                placeholder="请输入账号（4-32位字母/数字/下划线）"
                size="large"
              />
            </el-form-item>

            <el-form-item label="密码" prop="userPassword">
              <el-input
                v-model="loginForm.userPassword"
                type="password"
                placeholder="请输入密码（8-64位）"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item>
              <NbButton
                type="primary"
                block
                :loading="isLoading"
                @click="handleLogin"
              >
                登录
              </NbButton>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册 Tab -->
        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            class="login-form"
          >
            <el-form-item label="账号" prop="userAccount">
              <el-input
                v-model="registerForm.userAccount"
                placeholder="请输入账号（4-32位字母/数字/下划线）"
                size="large"
              />
            </el-form-item>

            <el-form-item label="密码" prop="userPassword">
              <el-input
                v-model="registerForm.userPassword"
                type="password"
                placeholder="请输入密码（8-64位）"
                size="large"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="checkPassword">
              <el-input
                v-model="registerForm.checkPassword"
                type="password"
                placeholder="请再次输入密码"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item>
              <NbButton
                type="primary"
                block
                :loading="isLoading"
                @click="handleRegister"
              >
                注册
              </NbButton>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </NbCard>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useUserStore } from '@/stores/user'
import { register as registerApi } from '@/api/user'
import type { LoginRequest, RegisterRequest } from '@/types/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const isLoading = ref(false)

// 登录表单
const loginFormRef = ref<FormInstance>()
const loginForm = reactive<LoginRequest>({
  userAccount: '',
  userPassword: '',
})

const loginRules: FormRules<LoginRequest> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 32, message: '账号长度为 4-32 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '账号只能包含字母、数字、下划线', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8-64 位', trigger: 'blur' },
  ],
}

// 注册表单
const registerFormRef = ref<FormInstance>()
const registerForm = reactive<RegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== registerForm.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules<RegisterRequest> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 32, message: '账号长度为 4-32 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '账号只能包含字母、数字、下划线', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8-64 位', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateCheckPassword, trigger: 'blur' },
  ],
}

// 登录处理
async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  isLoading.value = true
  try {
    const success = await userStore.login({
      userAccount: loginForm.userAccount,
      userPassword: loginForm.userPassword,
    })
    if (success) {
      ElMessage.success('登录成功')
      router.push('/')
    }
  } finally {
    isLoading.value = false
  }
}

// 注册处理
async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  isLoading.value = true
  try {
    const res = await registerApi({
      userAccount: registerForm.userAccount,
      userPassword: registerForm.userPassword,
      checkPassword: registerForm.checkPassword,
    })
    if (res.data.code === 0) {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      // 清空注册表单
      registerForm.userAccount = ''
      registerForm.userPassword = ''
      registerForm.checkPassword = ''
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-card {
  padding: 32px;
}

.login-form :deep(.el-form-item__label) {
  font-family: var(--font-heading);
  font-weight: 500;
  color: var(--nb-text);
  padding-bottom: 4px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add src/views/auth/LoginPage.vue
git commit -m "feat: add Login/Register page with form validation"
```

---

## Task 12: 创建首页

**Files:**
- Create: `frontend/src/views/home/HomePage.vue`

- [ ] **Step 1: 创建首页**

```vue
<!-- src/views/home/HomePage.vue -->
<template>
  <MainLayout>
    <div class="home-page">
      <!-- 欢迎卡片 -->
      <NbCard class="welcome-card">
        <div class="welcome-card__content">
          <div class="welcome-card__greeting">
            <span class="welcome-card__emoji">👋</span>
            <h2 class="welcome-card__title">
              欢迎回来，{{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || '求职者' }}
            </h2>
          </div>
          <p class="welcome-card__subtitle">
            今天准备挑战哪个方向的面试？让 AI 陪你练起来！
          </p>
        </div>
      </NbCard>

      <!-- 统计区域 -->
      <div class="home-page__stats">
        <NbCard hoverable class="stat-card">
          <div class="stat-card__value">0</div>
          <div class="stat-card__label">已完成面试</div>
        </NbCard>
        <NbCard hoverable class="stat-card">
          <div class="stat-card__value">0</div>
          <div class="stat-card__label">面试题目</div>
        </NbCard>
        <NbCard hoverable class="stat-card">
          <div class="stat-card__value">0</div>
          <div class="stat-card__label">练习天数</div>
        </NbCard>
      </div>

      <!-- 快捷操作 -->
      <div class="home-page__actions">
        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-primary);">
            <IconTarget />
          </div>
          <h3 class="action-card__title">开始面试</h3>
          <p class="action-card__desc">选择岗位方向，开始 AI 模拟面试</p>
          <NbButton type="primary" block>立即开始</NbButton>
        </NbCard>

        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-secondary);">
            <IconChart />
          </div>
          <h3 class="action-card__title">面试记录</h3>
          <p class="action-card__desc">查看历史面试记录和表现分析</p>
          <NbButton type="secondary" block>查看记录</NbButton>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import IconTarget from '@/components/icons/IconTarget.vue'
import IconChart from '@/components/icons/IconChart.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-card__content {
  text-align: center;
}

.welcome-card__greeting {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.welcome-card__emoji {
  font-size: 32px;
}

.welcome-card__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.welcome-card__subtitle {
  font-size: 16px;
  color: var(--nb-muted);
  margin: 0;
}

.home-page__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.stat-card {
  text-align: center;
  padding: 32px 24px;
}

.stat-card__value {
  font-family: var(--font-heading);
  font-size: 48px;
  font-weight: 700;
  color: var(--nb-primary);
  margin-bottom: 8px;
}

.stat-card__label {
  font-size: 14px;
  color: var(--nb-muted);
}

.home-page__actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.action-card {
  text-align: center;
}

.action-card__icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #fff;
}

.action-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
}

.action-card__desc {
  font-size: 14px;
  color: var(--nb-muted);
  margin-bottom: 20px;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .home-page__stats {
    grid-template-columns: 1fr;
  }

  .home-page__actions {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add src/views/home/HomePage.vue
git commit -m "feat: add HomePage skeleton with welcome card and stats"
```

---

## Task 13: 创建用户资料编辑页

**Files:**
- Create: `frontend/src/views/profile/ProfilePage.vue`

- [ ] **Step 1: 创建用户资料编辑页**

```vue
<!-- src/views/profile/ProfilePage.vue -->
<template>
  <MainLayout>
    <div class="profile-page">
      <h1 class="profile-page__title">个人资料</h1>

      <NbCard class="profile-card">
        <div class="profile-card__layout">
          <!-- 左侧头像区 -->
          <div class="profile-card__left">
            <div class="profile-avatar">
              {{ userStore.userInfo?.userName?.[0] || userStore.userInfo?.userAccount?.[0] || 'U' }}
            </div>
            <div class="profile-avatar__name">
              {{ userStore.userInfo?.userName || userStore.userInfo?.userAccount }}
            </div>
            <div class="profile-avatar__role">
              {{ userStore.userInfo?.userRole === 'admin' ? '管理员' : '普通用户' }}
            </div>
          </div>

          <!-- 右侧表单区 -->
          <div class="profile-card__right">
            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-position="top"
              class="profile-form"
            >
              <el-form-item label="昵称" prop="userName">
                <el-input v-model="form.userName" placeholder="请输入昵称" />
              </el-form-item>

              <el-form-item label="目标岗位" prop="targetPosition">
                <el-input v-model="form.targetPosition" placeholder="例如：Java 开发工程师" />
              </el-form-item>

              <el-form-item label="技术方向" prop="techDirection">
                <el-input v-model="form.techDirection" placeholder="例如：后端 / 前端 / 全栈" />
              </el-form-item>

              <el-form-item label="工作年限" prop="workYears">
                <el-input-number
                  v-model="form.workYears"
                  :min="0"
                  :max="60"
                  :step="1"
                  style="width: 100%;"
                  controls-position="right"
                />
              </el-form-item>

              <el-form-item label="城市" prop="city">
                <el-input v-model="form.city" placeholder="例如：北京" />
              </el-form-item>

              <el-form-item label="求职状态" prop="jobStatus">
                <el-select v-model="form.jobStatus" placeholder="请选择" style="width: 100%;">
                  <el-option label="不限" value="" />
                  <el-option label="正在看机会" value="looking" />
                  <el-option label="开放机会" value="open" />
                  <el-option label="暂不考虑" value="not_looking" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <NbButton
                  type="success"
                  block
                  :loading="isSaving"
                  @click="handleSave"
                >
                  保存资料
                </NbButton>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useUserStore } from '@/stores/user'
import type { UpdateProfileRequest } from '@/types/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const isSaving = ref(false)

const form = reactive<UpdateProfileRequest>({
  userName: '',
  targetPosition: '',
  techDirection: '',
  workYears: 0,
  city: '',
  jobStatus: '',
})

const rules: FormRules<UpdateProfileRequest> = {
  userName: [
    { max: 64, message: '昵称最长 64 个字符', trigger: 'blur' },
  ],
  targetPosition: [
    { max: 128, message: '目标岗位最长 128 个字符', trigger: 'blur' },
  ],
  techDirection: [
    { max: 128, message: '技术方向最长 128 个字符', trigger: 'blur' },
  ],
  workYears: [
    { type: 'number', min: 0, max: 60, message: '工作年限需在 0-60 之间', trigger: 'change' },
  ],
  city: [
    { max: 64, message: '城市最长 64 个字符', trigger: 'blur' },
  ],
}

// 页面加载时填充当前用户信息
onMounted(() => {
  if (userStore.userInfo) {
    form.userName = userStore.userInfo.userName || ''
    form.targetPosition = userStore.userInfo.targetPosition || ''
    form.techDirection = userStore.userInfo.techDirection || ''
    form.workYears = userStore.userInfo.workYears || 0
    form.city = userStore.userInfo.city || ''
    form.jobStatus = userStore.userInfo.jobStatus || ''
  }
})

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  isSaving.value = true
  try {
    const success = await userStore.updateProfile({
      userName: form.userName || undefined,
      targetPosition: form.targetPosition || undefined,
      techDirection: form.techDirection || undefined,
      workYears: form.workYears,
      city: form.city || undefined,
      jobStatus: form.jobStatus || undefined,
    })
    if (success) {
      ElMessage.success('资料保存成功')
    }
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.profile-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 24px;
}

.profile-card__layout {
  display: flex;
  gap: 48px;
}

.profile-card__left {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 24px;
  border-right: 2px dashed var(--nb-border);
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--nb-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: 48px;
  font-weight: 700;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  margin-bottom: 16px;
}

.profile-avatar__name {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.profile-avatar__role {
  font-size: 14px;
  color: var(--nb-muted);
}

.profile-card__right {
  flex: 1;
}

.profile-form :deep(.el-form-item__label) {
  font-family: var(--font-heading);
  font-weight: 500;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .profile-card__layout {
    flex-direction: column;
    gap: 24px;
  }

  .profile-card__left {
    flex: none;
    border-right: none;
    border-bottom: 2px dashed var(--nb-border);
    padding-top: 0;
    padding-bottom: 24px;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add src/views/profile/ProfilePage.vue
git commit -m "feat: add user profile edit page with all fields"
```

---

## Task 14: 更新 App.vue 和 main.ts

**Files:**
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/main.ts`

- [ ] **Step 1: 更新 App.vue**

```vue
<!-- src/App.vue -->
<template>
  <router-view />
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 2: 更新 main.ts**

```typescript
// src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

import './assets/styles/variables.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
```

- [ ] **Step 3: Commit**

```bash
git add src/App.vue src/main.ts
git commit -m "chore: wire up Element Plus, Pinia, Router and CSS variables"
```

---

## Task 15: 创建环境变量文件

**Files:**
- Create: `frontend/.env.development`
- Create: `frontend/.env.production`

- [ ] **Step 1: 创建环境变量**

```
# .env.development
VITE_API_BASE_URL=http://localhost:8080
```

```
# .env.production
VITE_API_BASE_URL=
```

- [ ] **Step 2: Commit**

```bash
git add .env.development .env.production
git commit -m "config: add environment variables for API base URL"
```

---

## Task 16: 运行类型检查和构建

**Files:**
- Modify: （根据 type-check 输出修复）

- [ ] **Step 1: 运行类型检查**

```bash
cd D:\code_schl\mianshiba\frontend
npm run type-check
```

Expected: 无类型错误。如有报错，逐条修复。

- [ ] **Step 2: 运行构建**

```bash
npm run build-only
```

Expected: 构建成功，`dist/` 目录生成。

- [ ] **Step 3: Commit（如有修复）**

```bash
git add .
git commit -m "fix: resolve TypeScript type errors"
```

---

## Task 17: 运行 Lint

**Files：**
- Modify: （根据 lint 输出修复）

- [ ] **Step 1: 运行 ESLint**

```bash
npm run lint
```

Expected: 无 lint 错误。

- [ ] **Step 2: Commit（如有修复）**

```bash
git add .
git commit -m "style: fix ESLint issues"
```

---

## 计划自检

| Spec 要求 | 对应任务 | 状态 |
|-----------|----------|------|
| 安装依赖（axios, element-plus, icons） | Task 1 | ✅ |
| Neubrutalism CSS 变量 + Element Plus 覆盖 | Task 2 | ✅ |
| TypeScript 类型定义 | Task 3 | ✅ |
| axios 实例 + 拦截器 | Task 4 | ✅ |
| 用户 API 层 | Task 5 | ✅ |
| Pinia 用户 Store | Task 6 | ✅ |
| 路由 + 导航守卫 | Task 7 | ✅ |
| SVG 图标组件 | Task 8 | ✅ |
| NbCard / NbButton 通用组件 | Task 9 | ✅ |
| AuthLayout（左右分屏） | Task 10 | ✅ |
| MainLayout（导航栏） | Task 10 | ✅ |
| 登录/注册页（Tab 切换 + 表单校验） | Task 11 | ✅ |
| 首页骨架 | Task 12 | ✅ |
| 用户资料编辑页 | Task 13 | ✅ |
| App.vue + main.ts 更新 | Task 14 | ✅ |
| 环境变量 | Task 15 | ✅ |
| 类型检查 + 构建 | Task 16 | ✅ |
| Lint 检查 | Task 17 | ✅ |

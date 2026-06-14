# 头像上传组件与个人资料导航重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 抽离可复用头像上传组件，移除个人资料页头像地址输入框，把顶部导航「个人资料」入口改为点击右侧头像进入。

**Architecture:** 新建通用 `NbAvatarUploader.vue` 组件统一处理预览、校验、上传；调整 `ProfilePage`、`MainLayout`、`BasicInfoEditor` 三处消费点；将通用上传接口迁移到 `api/file.ts` 保持调用兼容。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vite, Vitest

---

## Task 1: 迁移通用上传接口

**Files:**
- Create: `frontend/src/api/file.ts`
- Modify: `frontend/src/api/user.ts`

- [ ] **Step 1: 新建 `api/file.ts`**

```ts
// src/api/file.ts
import request from '@/utils/request'
import type { BaseResponse, FileUploadVO } from '@/types/user'

export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<BaseResponse<FileUploadVO>>('/api/file/avatar', formData)
}
```

- [ ] **Step 2: 修改 `api/user.ts` 保留兼容导出**

把原来的 `uploadAvatar` 函数删除，改为从 `api/file.ts` 重导出：

```ts
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

export { uploadImage as uploadAvatar } from './file'

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

- [ ] **Step 3: 验证编译**

Run: `cd frontend; npm run type-check`
Expected: 无 `uploadAvatar` 相关报错。

---

## Task 2: 创建 `NbAvatarUploader.vue`

**Files:**
- Create: `frontend/src/components/NbAvatarUploader.vue`
- Create: `frontend/src/components/__tests__/NbAvatarUploader.spec.ts`

- [ ] **Step 1: 编写组件失败测试（Vitest）**

```ts
// src/components/__tests__/NbAvatarUploader.spec.ts
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import NbAvatarUploader from '../NbAvatarUploader.vue'
import { ElMessage } from 'element-plus'

describe('NbAvatarUploader', () => {
  it('renders fallback text when no avatar', () => {
    const wrapper = mount(NbAvatarUploader, {
      props: {
        modelValue: '',
        uploadFn: vi.fn(),
        fallbackText: '张三',
      },
    })
    expect(wrapper.text()).toContain('张')
  })

  it('renders image when avatar is provided', () => {
    const wrapper = mount(NbAvatarUploader, {
      props: {
        modelValue: 'https://example.com/avatar.png',
        uploadFn: vi.fn(),
      },
    })
    expect(wrapper.find('img').exists()).toBe(true)
    expect(wrapper.find('img').attributes('src')).toBe('https://example.com/avatar.png')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd frontend; npx vitest run src/components/__tests__/NbAvatarUploader.spec.ts`
Expected: FAIL，组件未创建。

- [ ] **Step 3: 实现 `NbAvatarUploader.vue`**

```vue
<!-- src/components/NbAvatarUploader.vue -->
<template>
  <div class="nb-avatar-uploader" :style="rootStyle">
    <div
      class="nb-avatar-uploader__preview"
      :class="[`nb-avatar-uploader__preview--${shape}`]"
      :style="previewStyle"
      role="button"
      tabindex="0"
      @click="handleClick"
      @keydown.enter.space="handleClick"
    >
      <img
        v-if="modelValue"
        :src="modelValue"
        :alt="fallbackText || '头像'"
        class="nb-avatar-uploader__img"
      />
      <span v-else class="nb-avatar-uploader__fallback">{{ displayFallback }}</span>

      <div v-if="isUploading" class="nb-avatar-uploader__overlay">
        <span class="nb-avatar-uploader__spinner" />
      </div>
      <div v-else class="nb-avatar-uploader__overlay nb-avatar-uploader__overlay--hover">
        <span class="nb-avatar-uploader__hint">更换头像</span>
      </div>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      class="nb-avatar-uploader__input"
      :accept="accept"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

interface Props {
  modelValue: string
  uploadFn: (file: File) => Promise<string>
  fallbackText?: string
  size?: number
  shape?: 'circle' | 'rounded' | 'square'
  accept?: string
  maxSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  fallbackText: '',
  size: 120,
  shape: 'circle',
  accept: 'image/jpeg,image/png,image/webp',
  maxSize: 2 * 1024 * 1024,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'upload-success': [url: string]
  'upload-error': [error: unknown]
}>()

const fileInputRef = ref<HTMLInputElement>()
const isUploading = ref(false)

const rootStyle = computed(() => ({
  width: `${props.size}px`,
}))

const previewStyle = computed(() => ({
  width: `${props.size}px`,
  height: props.shape === 'rounded' ? `${props.size * 1.17}px` : `${props.size}px`,
}))

const displayFallback = computed(() => {
  if (props.fallbackText) return props.fallbackText.slice(0, 1)
  return 'U'
})

function handleClick() {
  if (isUploading.value) return
  fileInputRef.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!props.accept.split(',').includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、WebP 图片')
    resetInput(input)
    return
  }

  if (file.size > props.maxSize) {
    ElMessage.warning('头像不能超过 2MB')
    resetInput(input)
    return
  }

  isUploading.value = true
  try {
    const url = await props.uploadFn(file)
    emit('update:modelValue', url)
    emit('upload-success', url)
    ElMessage.success('头像上传成功')
  } catch (error) {
    emit('upload-error', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    isUploading.value = false
    resetInput(input)
  }
}

function resetInput(input: HTMLInputElement) {
  input.value = ''
}
</script>

<style scoped>
.nb-avatar-uploader {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

.nb-avatar-uploader__preview {
  position: relative;
  overflow: hidden;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  background: var(--nb-primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--nb-transition);
}

.nb-avatar-uploader__preview--circle {
  border-radius: 50%;
}

.nb-avatar-uploader__preview--rounded {
  border-radius: 10px;
}

.nb-avatar-uploader__preview--square {
  border-radius: var(--nb-radius);
}

.nb-avatar-uploader__preview:hover {
  box-shadow: var(--nb-shadow-sm);
}

.nb-avatar-uploader__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.nb-avatar-uploader__fallback {
  font-family: var(--font-heading);
  font-size: calc(v-bind('props.size') * 0.4px);
  font-weight: 700;
}

.nb-avatar-uploader__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 200ms ease;
}

.nb-avatar-uploader__preview:hover .nb-avatar-uploader__overlay--hover,
.nb-avatar-uploader__preview:focus-visible .nb-avatar-uploader__overlay--hover {
  opacity: 1;
}

.nb-avatar-uploader__overlay:not(.nb-avatar-uploader__overlay--hover) {
  opacity: 1;
}

.nb-avatar-uploader__hint {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.nb-avatar-uploader__spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: nb-avatar-uploader-spin 1s linear infinite;
}

@keyframes nb-avatar-uploader-spin {
  to {
    transform: rotate(360deg);
  }
}

.nb-avatar-uploader__input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
</style>
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd frontend; npx vitest run src/components/__tests__/NbAvatarUploader.spec.ts`
Expected: PASS。

- [ ] **Step 5: 运行 lint**

Run: `cd frontend; npm run lint`
Expected: 无新增 lint 错误。

---

## Task 3: 重构 `ProfilePage.vue`

**Files:**
- Modify: `frontend/src/views/profile/ProfilePage.vue`

- [ ] **Step 1: 左侧头像区替换为 `NbAvatarUploader`**

把：

```vue
<div class="profile-avatar">
  <img v-if="form.userAvatar" :src="form.userAvatar" alt="用户头像" />
  <span v-else>{{ userStore.userInfo?.userName?.[0] || userStore.userInfo?.userAccount?.[0] || 'U' }}</span>
</div>
<input
  ref="avatarInputRef"
  class="profile-avatar__input"
  type="file"
  accept="image/jpeg,image/png,image/webp"
  @change="handleAvatarChange"
/>
<NbButton variant="ghost" :loading="isUploadingAvatar" @click="avatarInputRef?.click()">
  上传头像
</NbButton>
```

替换为：

```vue
<NbAvatarUploader
  v-model="form.userAvatar"
  :upload-fn="uploadAvatarAndReturnUrl"
  :fallback-text="userStore.userInfo?.userName || userStore.userInfo?.userAccount"
  :size="120"
/ />
```

- [ ] **Step 2: 删除「头像地址」表单项**

删除：

```vue
<el-form-item label="头像地址" prop="userAvatar">
  <el-input v-model="form.userAvatar" placeholder="上传后自动填充，也可粘贴图片 URL" />
</el-form-item>
```

- [ ] **Step 3: 移除废弃的导入、变量与函数**

删除：
- `import { uploadAvatar } from '@/api/user'` 改为保留 `uploadAvatar` 导入用于上传
- `const avatarInputRef = ref<HTMLInputElement>()`
- `const isUploadingAvatar = ref(false)`
- `handleAvatarChange` 函数

新增上传包装函数（放在 script 中合适位置）：

```ts
import { uploadAvatar } from '@/api/user'

async function uploadAvatarAndReturnUrl(file: File): Promise<string> {
  const res = await uploadAvatar(file)
  return res.data.url
}
```

- [ ] **Step 4: 删除废弃样式**

删除 `.profile-avatar`、`.profile-avatar__input` 相关样式，保留 `.profile-avatar__name`、`.profile-avatar__role`。

- [ ] **Step 5: 验证类型检查**

Run: `cd frontend; npm run type-check`
Expected: 无错误。

---

## Task 4: 重构 `MainLayout.vue` 导航

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: 从 `navItems` 移除「个人资料」**

```ts
const navItems: NavItem[] = [
  { label: '首页', to: '/', group: 'core' },
  { label: '我的简历', to: '/resume', group: 'core' },
  { label: '模拟面试', to: '/interview', group: 'core' },
  { label: '职位情报', to: '/job/recommendations', group: 'core' },
  { label: '投递管理', to: '/applications', group: 'growth' },
  { label: '训练中心', to: '/training', group: 'growth' },
  { label: '求职教练', to: '/coach', group: 'growth' },
  { label: '管理后台', to: '/admin', group: 'account', adminOnly: true },
]
```

- [ ] **Step 2: 拆分头像与下拉菜单**

把：

```vue
<el-dropdown v-if="userStore.isLoggedIn" trigger="click">
  <div class="main-layout__user-trigger">
    <div class="main-layout__avatar">
      <img v-if="avatarUrl" :src="avatarUrl" :alt="displayName" class="main-layout__avatar-img" />
      <span v-else class="main-layout__avatar-initial">{{ avatarInitial }}</span>
    </div>
    <span class="main-layout__username">{{ displayName }}</span>
  </div>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item @click="$router.push('/profile')">个人资料</el-dropdown-item>
      <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
```

替换为：

```vue
<div v-if="userStore.isLoggedIn" class="main-layout__user">
  <router-link to="/profile" class="main-layout__avatar-link" aria-label="进入个人资料">
    <div class="main-layout__avatar">
      <img v-if="avatarUrl" :src="avatarUrl" :alt="displayName" class="main-layout__avatar-img" />
      <span v-else class="main-layout__avatar-initial">{{ avatarInitial }}</span>
    </div>
  </router-link>

  <el-dropdown trigger="click">
    <div class="main-layout__user-trigger">
      <span class="main-layout__username">{{ displayName }}</span>
      <span class="main-layout__dropdown-arrow" />
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</div>
```

- [ ] **Step 3: 补充/调整样式**

新增/修改样式（保持 Neubrutalism 风格）：

```css
.main-layout__user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-layout__avatar-link {
  display: flex;
  text-decoration: none;
}

.main-layout__avatar-link:hover .main-layout__avatar {
  border-color: var(--nb-primary);
}

.main-layout__user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--nb-radius);
  transition: var(--nb-transition);
}

.main-layout__dropdown-arrow {
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid var(--nb-muted);
}
```

- [ ] **Step 4: 验证类型检查**

Run: `cd frontend; npm run type-check`
Expected: 无错误。

---

## Task 5: 重构 `BasicInfoEditor.vue`

**Files:**
- Modify: `frontend/src/components/resume/sections/BasicInfoEditor.vue`

- [ ] **Step 1: 导入 `NbAvatarUploader` 和 `uploadImage`**

```ts
import { computed } from 'vue'
import NbAvatarUploader from '@/components/NbAvatarUploader.vue'
import { uploadImage } from '@/api/file'
```

- [ ] **Step 2: 替换头像 URL 输入框**

把：

```vue
<div class="basic-info-editor__avatar">
  <div class="basic-info-editor__avatar-preview">
    <img v-if="formData.avatar" :src="String(formData.avatar)" alt="头像预览" />
    <span v-else>{{ String(formData.name || '你').slice(0, 1) }}</span>
  </div>
  <el-form-item label="头像 URL" class="basic-info-editor__avatar-input">
    <el-input v-model="formData.avatar" placeholder="粘贴头像图片链接，可选" />
  </el-form-item>
</div>
```

替换为：

```vue
<div class="basic-info-editor__avatar">
  <NbAvatarUploader
    v-model="formData.avatar"
    :upload-fn="uploadImageAndReturnUrl"
    :fallback-text="String(formData.name || '你')"
    :size="96"
    shape="rounded"
  />
  <div class="basic-info-editor__avatar-hint">
    点击头像上传图片，支持 JPG、PNG、WebP，最大 2MB
  </div>
</div>
```

- [ ] **Step 3: 添加上传包装函数**

```ts
async function uploadImageAndReturnUrl(file: File): Promise<string> {
  const res = await uploadImage(file)
  return res.data.url
}
```

- [ ] **Step 4: 调整样式**

删除 `.basic-info-editor__avatar-preview`、`.basic-info-editor__avatar-preview img`、`.basic-info-editor__avatar-input` 样式。

新增：

```css
.basic-info-editor__avatar-hint {
  flex: 1;
  font-size: 13px;
  color: var(--nb-muted);
  line-height: 1.5;
}
```

- [ ] **Step 5: 验证类型检查**

Run: `cd frontend; npm run type-check`
Expected: 无错误。

---

## Task 6: 全局验证

- [ ] **Step 1: 运行 lint**

Run: `cd frontend; npm run lint`
Expected: 无新增错误。

- [ ] **Step 2: 运行单元测试**

Run: `cd frontend; npm run test:unit`
Expected: 全部通过。

- [ ] **Step 3: 运行构建**

Run: `cd frontend; npm run build`
Expected: 构建成功。

---

## Self-Review Checklist

- [ ] Spec 中「移除头像地址输入框」已对应 Task 3 Step 2。
- [ ] Spec 中「头像点击进个人资料」已对应 Task 4 Step 2。
- [ ] Spec 中「去掉主导航个人资料」已对应 Task 4 Step 1。
- [ ] Spec 中「简历编辑器复用」已对应 Task 5。
- [ ] 无 TBD / TODO / 占位符。
- [ ] `uploadAvatar` 与 `uploadImage` 命名一致。

# 头像上传组件与个人资料导航重构设计

## 背景

当前个人资料页（`ProfilePage.vue`）的头像上传逻辑与表单代码耦合在一起，且表单中直接展示了「头像地址」输入框，对普通用户不友好。顶部主导航栏中同时存在「个人资料」入口，与右侧头像区域的入口重复。

## 目标

1. 抽离出一个可复用的头像上传组件，用于个人资料页和简历基础信息编辑器。
2. 从个人资料表单中移除「头像地址」输入框。
3. 从顶部主导航栏中移除「个人资料」入口，改为点击右侧头像进入个人资料页。

## 方案

采用「通用头像上传组件」方案：新建 `NbAvatarUploader.vue`，并在 `ProfilePage`、`MainLayout`、`BasicInfoEditor` 中做对应改造。

## 组件设计

### `src/components/NbAvatarUploader.vue`

通用头像上传组件，负责预览、文件校验、上传状态、错误提示。

#### Props

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `modelValue` | `string` | 是 | - | 当前头像 URL |
| `uploadFn` | `(file: File) => Promise<string>` | 是 | - | 上传函数，需返回头像 URL |
| `fallbackText` | `string` | 否 | 取第一个字符 | 无头像时显示的文字 |
| `size` | `number` | 否 | `120` | 预览区域尺寸（px） |
| `shape` | `'circle' \| 'rounded' \| 'square'` | 否 | `'circle'` | 预览形状 |
| `accept` | `string` | 否 | `image/jpeg,image/png,image/webp` | 接受的文件类型 |
| `maxSize` | `number` | 否 | `2 * 1024 * 1024` | 最大文件大小（字节） |

#### Emits

| 事件 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `value: string` | 上传成功后回写 URL |
| `upload-success` | `url: string` | 上传成功 |
| `upload-error` | `error: unknown` | 上传失败 |

#### 交互

- 点击预览区域触发隐藏的文件选择框。
- 选择文件后校验类型和大小，不通过则 `ElMessage.warning` 提示并重置 input。
- 校验通过后调用 `uploadFn`，期间预览区显示 Loading 覆盖层。
- 上传成功通过 `update:modelValue` 回写 URL，并 `emit('upload-success', url)`。
- 上传失败通过 `ElMessage.error` 提示，并 `emit('upload-error', error)`。
- 鼠标悬停预览区时显示半透明遮罩与「更换头像」提示。

## 页面改造

### `ProfilePage.vue`

- 左侧头像区替换为 `<NbAvatarUploader>`，绑定 `form.userAvatar`。
- 删除表单中的「头像地址」`el-form-item`。
- 保留左侧的用户昵称、角色展示。
- 保存资料时继续提交 `userAvatar` 字段。

### `MainLayout.vue`

- 从 `navItems` 中删除 `{ label: '个人资料', to: '/profile', group: 'account' }`。
- 右侧用户区拆分为：
  - **头像**：使用 `router-link` 或点击事件跳转到 `/profile`。
  - **用户名 + 下拉箭头**：保留 `el-dropdown`，下拉菜单仅保留「退出登录」。
- 移动端菜单同步移除「个人资料」项。

### `BasicInfoEditor.vue`

- 将「头像 URL」输入框替换为 `<NbAvatarUploader shape="rounded" :size="96">`，绑定 `formData.avatar`。
- 复用通用上传接口（见下文）。

## API 调整

### `src/api/file.ts`（新建）

将原本位于 `api/user.ts` 的通用文件上传逻辑迁移到 `api/file.ts`：

```ts
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<BaseResponse<FileUploadVO>>('/api/file/avatar', formData)
}
```

### `src/api/user.ts`

保留 `uploadAvatar` 作为兼容导出，避免已有调用方报错：

```ts
export { uploadImage as uploadAvatar } from './file'
```

## 错误处理

- 文件类型、大小不合法：组件内拦截并提示，不触发上传。
- 上传接口失败：组件内 `ElMessage.error` 提示，不中断表单填写。
- 保存资料失败：保持现有 `userStore.updateProfile` 的错误处理。

## 验收标准

- [ ] `NbAvatarUploader.vue` 组件可正常渲染、选择文件、上传并回显。
- [ ] 个人资料页不再展示「头像地址」输入框。
- [ ] 顶部主导航栏不再展示「个人资料」入口。
- [ ] 点击顶部右侧头像可进入 `/profile`。
- [ ] 顶部右侧下拉菜单仅保留「退出登录」。
- [ ] 简历基础信息编辑器的头像输入替换为上传组件。
- [ ] 现有 `uploadAvatar` 调用方不受影响。

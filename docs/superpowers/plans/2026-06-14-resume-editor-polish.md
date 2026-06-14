# Resume Editor Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the resume editor UI so layout, avatar upload, dropdown fields, project description formatting, contact icons, skill tags, and date picker locale match the requested behavior.

**Architecture:** Keep all changes frontend-only and preserve existing resume section data shapes. Edit the existing Vue components in place, add small local helpers where needed, and configure Element Plus locale globally.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, Element Plus, existing Neubrutalism CSS variables, Vite.

---

## File Structure

- Modify: `frontend/src/main.ts` - register Element Plus with Chinese locale.
- Modify: `frontend/src/views/resume/ResumeEditPage.vue` - fix editor/preview/AI panel layout and scroll boundaries.
- Modify: `frontend/src/components/resume/sections/BasicInfoEditor.vue` - avatar upload, status select, city select.
- Modify: `frontend/src/components/resume/sections/ProjectEditor.vue` - lightweight formatting toolbar for project description.
- Modify: `frontend/src/components/resume/sections/SkillsEditor.vue` - compact skill tag/card styling.
- Modify: `frontend/src/templates/MinimalTech.vue` - contact icons in default preview template.
- Modify: `frontend/src/templates/ModernTwoCol.vue` - contact icons in split template.
- Modify: `frontend/src/templates/ClassicFormal.vue` - contact icons in classic template.

## Task 1: Element Plus Chinese Locale

**Files:**
- Modify: `frontend/src/main.ts`

- [ ] **Step 1: Inspect current app registration**

Run: `Get-Content -LiteralPath "frontend\src\main.ts"`

Expected: `app.use(ElementPlus)` is present without locale options.

- [ ] **Step 2: Configure locale**

Change `frontend/src/main.ts` to import Chinese locale and pass it to Element Plus:

```ts
import zhCn from 'element-plus/es/locale/lang/zh-cn'

app.use(ElementPlus, {
  locale: zhCn,
})
```

- [ ] **Step 3: Verify TypeScript compile**

Run: `npm run type-check` from `frontend/`.

Expected: command exits successfully.

## Task 2: Resume Editor Layout Boundaries

**Files:**
- Modify: `frontend/src/views/resume/ResumeEditPage.vue`

- [ ] **Step 1: Update right panel layout CSS**

Keep the existing template and adjust CSS so the preview and AI panel do not overlap:

```css
.rep-aside {
  width: calc(57% - 230px);
  min-width: 520px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: var(--nb-bg);
}

.rep-preview {
  flex: 1 1 58%;
  min-height: 0;
  overflow: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rep-ai-panel {
  flex: 0 0 320px;
  min-height: 240px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: var(--nb-radius-lg) var(--nb-radius-lg) 0 0;
  margin: 0 16px 0 0;
}
```

- [ ] **Step 2: Keep mobile behavior safe**

Inside the existing `@media (max-width: 960px)` block, ensure `.rep-preview` and `.rep-ai-panel` can expand naturally:

```css
.rep-preview {
  overflow: visible;
}

.rep-ai-panel {
  flex: none;
  min-height: 360px;
  max-height: none;
  margin: 16px;
  border-radius: var(--nb-radius-lg);
}
```

- [ ] **Step 3: Verify visually**

Run: `npm run dev` from `frontend/`, open the resume edit page, scroll both left editor and right preview.

Expected: no bottom overlap between preview content and AI panel; each panel remains usable.

## Task 3: Basic Info Upload And Dropdowns

**Files:**
- Modify: `frontend/src/components/resume/sections/BasicInfoEditor.vue`

- [ ] **Step 1: Add upload UI**

Replace the avatar URL form item with a hidden file input and action buttons:

```vue
<input
  ref="avatarInputRef"
  class="basic-info-editor__avatar-file"
  type="file"
  accept="image/png,image/jpeg,image/webp,image/gif"
  @change="handleAvatarChange"
/>
<div class="basic-info-editor__avatar-actions">
  <el-button type="primary" plain @click="avatarInputRef?.click()">
    {{ formData.avatar ? '更换头像' : '上传头像' }}
  </el-button>
  <el-button v-if="formData.avatar" plain @click="removeAvatar">移除头像</el-button>
  <span>支持 PNG/JPG/WebP/GIF，最大 2MB</span>
</div>
```

- [ ] **Step 2: Add status select**

Replace `currentStatus` input with editable select:

```vue
<el-select v-model="formData.currentStatus" filterable allow-create default-first-option placeholder="请选择或输入当前状态" style="width: 100%;">
  <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
</el-select>
```

- [ ] **Step 3: Add city select**

Replace `city` input with editable select:

```vue
<el-select v-model="formData.city" filterable allow-create default-first-option placeholder="请选择或输入所在城市" style="width: 100%;">
  <el-option v-for="item in cityOptions" :key="item" :label="item" :value="item" />
</el-select>
```

- [ ] **Step 4: Add script helpers**

Import `ref` and `ElMessage`, then add file validation and mutation helpers:

```ts
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const avatarInputRef = ref<HTMLInputElement>()
const maxAvatarSize = 2 * 1024 * 1024
const allowedAvatarTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
const statusOptions = ['应届毕业生', '在校生', '已离职', '在职-看机会', '在职-暂不考虑']
const cityOptions = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京', '苏州', '西安', '重庆', '远程']

function updateField(key: string, value: unknown) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!allowedAvatarTypes.includes(file.type)) {
    ElMessage.warning('请上传 PNG、JPG、WebP 或 GIF 图片')
    input.value = ''
    return
  }
  if (file.size > maxAvatarSize) {
    ElMessage.warning('头像不能超过 2MB')
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => updateField('avatar', reader.result || '')
  reader.readAsDataURL(file)
  input.value = ''
}

function removeAvatar() {
  updateField('avatar', '')
}
```

- [ ] **Step 5: Add CSS for upload controls**

```css
.basic-info-editor__avatar-file {
  display: none;
}

.basic-info-editor__avatar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
  color: var(--nb-muted);
  font-size: 12px;
}
```

- [ ] **Step 6: Verify manually**

Open resume edit page and upload a valid image, upload an invalid file, select status, and select city.

Expected: valid image previews immediately, invalid file shows warning, select fields write strings into the form.

## Task 4: Project Description Toolbar

**Files:**
- Modify: `frontend/src/components/resume/sections/ProjectEditor.vue`

- [ ] **Step 1: Add toolbar template**

Replace the simple project description form item with toolbar plus textarea:

```vue
<el-form-item label="项目描述">
  <div class="project-editor__formatbar">
    <el-button size="small" plain @click="insertDescriptionFormat(index, 'bold')">加粗</el-button>
    <el-button size="small" plain @click="insertDescriptionFormat(index, 'unordered')">无序列表</el-button>
    <el-button size="small" plain @click="insertDescriptionFormat(index, 'ordered')">有序列表</el-button>
  </div>
  <el-input
    :ref="(el: unknown) => setDescriptionInputRef(el, index)"
    v-model="item.description"
    type="textarea"
    :rows="5"
    placeholder="请描述项目内容，可用工具按钮快速插入列表和加粗标记"
  />
</el-form-item>
```

- [ ] **Step 2: Add textarea refs and insert helpers**

Add helpers that update the item immutably:

```ts
const descriptionInputRefs = ref<Record<number, HTMLTextAreaElement | null>>({})

function setDescriptionInputRef(el: unknown, index: number) {
  const textarea = (el as { textarea?: HTMLTextAreaElement } | null)?.textarea || null
  descriptionInputRefs.value[index] = textarea
}

function insertDescriptionFormat(itemIndex: number, type: 'bold' | 'unordered' | 'ordered') {
  const snippets = {
    bold: '**重点内容**',
    unordered: '- 项目亮点',
    ordered: '1. 项目步骤',
  }
  const textarea = descriptionInputRefs.value[itemIndex]
  const item = getItems()[itemIndex]
  if (!item) return
  const text = String(item.description || '')
  const snippet = snippets[type]
  const start = textarea?.selectionStart ?? text.length
  const end = textarea?.selectionEnd ?? text.length
  const prefix = start > 0 && !text.slice(0, start).endsWith('\n') ? '\n' : ''
  const nextDescription = `${text.slice(0, start)}${prefix}${snippet}${text.slice(end)}`
  const newList = [...getItems()]
  newList[itemIndex] = { ...item, description: nextDescription }
  emit('update:items', newList)
  nextTick(() => {
    textarea?.focus()
    const cursor = start + prefix.length + snippet.length
    textarea?.setSelectionRange(cursor, cursor)
  })
}
```

- [ ] **Step 3: Add toolbar CSS**

```css
.project-editor__formatbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
```

- [ ] **Step 4: Verify manually**

Open a project description, click each toolbar button, and type after the inserted marker.

Expected: markers insert at cursor or append at the end without breaking save data.

## Task 5: Skill Tag Visual Polish

**Files:**
- Modify: `frontend/src/components/resume/sections/SkillsEditor.vue`

- [ ] **Step 1: Adjust category and tag CSS**

Add or update styles:

```css
.skill-category {
  padding: 14px !important;
}

.skill-category__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.skill-tag {
  height: 26px;
  padding: 0 8px;
  border: 1px solid #dbe1ff;
  box-shadow: 0 2px 6px rgba(17, 24, 39, 0.06);
  background: #f3f5ff;
  color: var(--nb-primary);
  font-size: 12px;
  font-weight: 600;
}

.skill-input {
  width: 132px;
}
```

- [ ] **Step 2: Verify manually**

Add one category and several skills.

Expected: tags are compact, aligned, and do not dominate the card.

## Task 6: Contact Icons In Templates

**Files:**
- Modify: `frontend/src/templates/MinimalTech.vue`
- Modify: `frontend/src/templates/ModernTwoCol.vue`
- Modify: `frontend/src/templates/ClassicFormal.vue`

- [ ] **Step 1: Add inline icon helper data per template**

In each template script, add a small `contactIconMap` string map or computed contact rows. Use inline SVG through CSS masks or direct template SVG. Prefer direct template spans with a reusable class to avoid external assets.

- [ ] **Step 2: Update contact markup**

Wrap each contact value with a row/span containing an icon and text, for example:

```vue
<span v-if="basic?.phone" class="ats-contact-item">
  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6.6 10.8c1.4 2.8 3.8 5.2 6.6 6.6l2.2-2.2c.3-.3.7-.4 1.1-.3 1.2.4 2.5.6 3.8.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1C10.8 21 3 13.2 3 3.7c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.6.6 3.8.1.4 0 .8-.3 1.1l-2.2 2.2z" /></svg>
  {{ basic.phone }}
</span>
```

- [ ] **Step 3: Add template-specific icon CSS**

For `MinimalTech.vue`:

```css
.ats-contact-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ats-contact-item svg {
  width: 12px;
  height: 12px;
  fill: #3f6df6;
  flex-shrink: 0;
}
```

For `ModernTwoCol.vue` and `ClassicFormal.vue`, use equivalent `display: inline-flex`, `gap`, `width`, `height`, and template colors.

- [ ] **Step 4: Verify manually**

Switch between all three templates.

Expected: phone/email/status/location/link fields display small readable icons without changing the template's overall style.

## Task 7: Frontend Verification

**Files:**
- No source file changes unless verification reveals issues.

- [ ] **Step 1: Run type check**

Run from `frontend/`: `npm run type-check`

Expected: command exits successfully.

- [ ] **Step 2: Run build**

Run from `frontend/`: `npm run build-only`

Expected: Vite build exits successfully.

- [ ] **Step 3: Manual smoke test**

Run from `frontend/`: `npm run dev`

Check the resume edit page:

- Avatar upload previews and delete works.
- Current status and city dropdowns can select and type custom values.
- Project description toolbar inserts markers.
- Right preview and AI panel do not overlap.
- Skill tags look compact.
- Date picker month names are Chinese.
- All three templates show contact icons.

Expected: all listed behaviors work.

## Self-Review

- Spec coverage: all seven requested fixes are mapped to Tasks 1-7.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: existing resume field names are preserved (`avatar`, `currentStatus`, `city`, `description`, `categories`).
- Scope check: plan is frontend-only and does not add backend upload, schema migration, or rich text dependency.

# PatchCompareCard Implementation Plan

> **For agentic workers:** Sub-task: Use subagent-driven-development (recommended) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Replace the existing "查看对比 → JSON 弹窗" flow with an inline expandable comparison card inside the AI chat panel, with "同意"/"反对" buttons.

**Architecture:** New `PatchCompareCard.vue` component handles both collapsed (mini proposal card) and expanded (field-by-field comparison) states. Integrated into `AiChatPanel.vue` via a new `sectionDataMap` prop from `ResumeEditPage.vue`.

**Tech Stack:** Vue 3 + TypeScript + Vitest

---

### Task 1: Remove deprecated ResumePatchConfirmDialog usage from ResumeEditPage

**Files:**
- Modify: `src/views/resume/ResumeEditPage.vue`

The current `handlePatchProposal` opens a dialog. In the new flow, clicking "同意" directly applies the proposal, so we change this to skip the dialog.

- [ ] **Step 1: Update `handlePatchProposal` to directly apply**

Replace the body of `handlePatchProposal`:

```ts
function handlePatchProposal(proposal: ResumePatchProposal) {
  if (proposal.operation !== 'replace_section') {
    ElMessage.warning('暂不支持该 AI 修改类型')
    return
  }
  handlePatchProposalApplied(proposal)
}
```

Also mark `patchConfirmVisible` and `pendingPatchProposal` refs as unused (they'll be cleaned up later).

- [ ] **Step 2: Run type-check to confirm no breakage**

```bash
npm run type-check
```
Expected: exit 0

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/resume/ResumeEditPage.vue
git commit -m "refactor: replace dialog flow with inline apply in handlePatchProposal"
```

---

### Task 2: Write PatchCompareCard component

**Files:**
- Create: `src/components/resume/PatchCompareCard.vue`
- Create: `src/components/resume/__tests__/PatchCompareCard.spec.ts`

This is the core component. It receives a proposal and the current section data, and renders either a collapsed mini card or an expanded comparison card.

- [ ] **Step 1: Write the failing test**

Create `src/components/resume/__tests__/PatchCompareCard.spec.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PatchCompareCard from '../PatchCompareCard.vue'

const mockProposal = {
  sectionType: 'basic' as const,
  operation: 'replace_section' as const,
  reason: '基本信息可以更规范',
  sectionData: {
    name: '张三',
    email: 'zhangsan@xx.com',
    phone: '13800138000',
    targetPosition: 'Java 后端工程师',
    city: '北京',
    github: '',
    blog: '',
  },
}

const mockCurrentData = {
  name: '张三',
  email: 'a@b.com',
  phone: '138001',
  targetPosition: '',
  city: '',
  github: '',
  blog: '',
}

describe('PatchCompareCard', () => {
  it('renders collapsed mini card by default', () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: true },
      },
    })
    expect(wrapper.find('.patch-compare-card__mini').exists()).toBe(true)
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(false)
  })

  it('expands to show comparison when clicking 查看对比', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(true)
    expect(wrapper.find('.patch-compare-card__field').exists()).toBe(true)
  })

  it('shows modified fields with markers', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    const modifiedRows = wrapper.findAll('.patch-compare-card__field--modified')
    expect(modifiedRows.length).toBeGreaterThanOrEqual(1)
  })

  it('emits accept on clicking 同意', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    await wrapper.find('.patch-compare-card__accept-btn').trigger('click')
    expect(wrapper.emitted('accept')).toHaveLength(1)
  })

  it('emits reject on clicking 反对', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    await wrapper.find('.patch-compare-card__reject-btn').trigger('click')
    expect(wrapper.emitted('reject')).toHaveLength(1)
  })

  it('collapses back when clicking 收起', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(true)
    await wrapper.find('.patch-compare-card__collapse-btn').trigger('click')
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(false)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npm run test:unit -- src/components/resume/__tests__/PatchCompareCard.spec.ts
```
Expected: FAIL — component not found or missing class names

- [ ] **Step 3: Write PatchCompareCard component**

Create `src/components/resume/PatchCompareCard.vue`:

```vue
<template>
  <div class="patch-compare-card">
    <!-- Collapsed mini card -->
    <div v-if="!expanded" class="patch-compare-card__mini">
      <span class="patch-compare-card__mini-title">AI 建议修改{{ sectionLabel }}</span>
      <small class="patch-compare-card__mini-reason">{{ proposal.reason || '等待确认后应用' }}</small>
      <div class="patch-compare-card__mini-actions">
        <NbButton variant="primary" size="small" class="patch-compare-card__expand-btn" @click="expanded = true">查看对比</NbButton>
        <NbButton variant="ghost" size="small" @click="$emit('reject')">忽略</NbButton>
      </div>
    </div>

    <!-- Expanded comparison card -->
    <div v-else class="patch-compare-card__compare">
      <div class="patch-compare-card__header">
        <span class="patch-compare-card__header-title">{{ sectionLabel }} · AI 建议修改</span>
        <NbButton variant="ghost" size="small" class="patch-compare-card__collapse-btn" @click="expanded = false">收起</NbButton>
      </div>
      <p class="patch-compare-card__reason">{{ proposal.reason }}</p>

      <div class="patch-compare-card__columns">
        <div class="patch-compare-card__side">
          <div class="patch-compare-card__side-title">当前内容</div>
          <div class="patch-compare-card__fields">
            <div
              v-for="field in fields"
              :key="field.key"
              :class="['patch-compare-card__field', `patch-compare-card__field--${field.status}`]"
            >
              <span class="patch-compare-card__field-label">{{ fieldLabel(field.key) }}</span>
              <span class="patch-compare-card__field-value patch-compare-card__field-value--current">{{ field.currentValue || '无' }}</span>
            </div>
          </div>
        </div>
        <div class="patch-compare-card__side patch-compare-card__side--proposed">
          <div class="patch-compare-card__side-title">AI 建议</div>
          <div class="patch-compare-card__fields">
            <div
              v-for="field in fields"
              :key="field.key"
              :class="['patch-compare-card__field', `patch-compare-card__field--${field.status}`]"
            >
              <span class="patch-compare-card__field-label">{{ fieldLabel(field.key) }}</span>
              <span class="patch-compare-card__field-value patch-compare-card__field-value--proposed">{{ field.proposedValue || '无' }}</span>
              <span v-if="field.status === 'modified'" class="patch-compare-card__tag patch-compare-card__tag--modified">修改</span>
              <span v-if="field.status === 'added'" class="patch-compare-card__tag patch-compare-card__tag--added">新增</span>
              <span v-if="field.status === 'removed'" class="patch-compare-card__tag patch-compare-card__tag--removed">删除</span>
            </div>
          </div>
        </div>
      </div>

      <div class="patch-compare-card__footer">
        <NbButton variant="primary" class="patch-compare-card__accept-btn" @click="$emit('accept')">同意</NbButton>
        <NbButton variant="ghost" class="patch-compare-card__reject-btn" @click="handleReject">反对</NbButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ResumePatchProposal, SectionType } from '@/types/resume'
import NbButton from '@/components/NbButton.vue'

const props = defineProps<{
  proposal: ResumePatchProposal
  currentData: Record<string, unknown> | Record<string, unknown>[]
  sectionType: SectionType
}>()

const emit = defineEmits<{
  accept: []
  reject: []
}>()

const expanded = ref(false)

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

const sectionLabel = computed(() => sectionLabelMap[props.sectionType] || props.sectionType)

const fieldLabelMap: Record<string, string> = {
  name: '姓名',
  email: '邮箱',
  phone: '电话',
  targetPosition: '意向岗位',
  city: '所在城市',
  github: 'GitHub',
  blog: '博客',
  avatar: '头像',
  currentStatus: '当前状态',
  expectedLocation: '期望地点',
  expectedSalary: '期望薪资',
  wechat: '微信',
  website: '个人网站',
  school: '学校',
  major: '专业',
  degree: '学历',
  startDate: '开始时间',
  endDate: '结束时间',
  gpa: 'GPA',
  activities: '校园活动',
  highlights: '亮点',
  company: '公司',
  position: '职位',
  description: '描述',
  name: '项目名称',
  role: '角色',
  techStack: '技术栈',
  content: '个人简介',
  categories: '技能分类',
}

function fieldLabel(key: string): string {
  return fieldLabelMap[key] || key
}

interface FieldDiff {
  key: string
  currentValue: string
  proposedValue: string
  status: 'same' | 'modified' | 'added' | 'removed'
}

function formatValue(v: unknown): string {
  if (v === undefined || v === null) return ''
  if (Array.isArray(v)) {
    const items = v.map((i) => (typeof i === 'object' ? JSON.stringify(i) : String(i)))
    return items.join(', ')
  }
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

const fields = computed<FieldDiff[]>(() => {
  const current = Array.isArray(props.currentData) ? {} : (props.currentData as Record<string, unknown>)
  const proposed = props.proposal.sectionData || {}
  const allKeys = new Set([...Object.keys(current), ...Object.keys(proposed)])
  const result: FieldDiff[] = []

  for (const key of allKeys) {
    const cv = current[key]
    const pv = proposed[key]
    const cStr = formatValue(cv)
    const pStr = formatValue(pv)

    if (!cStr && !pStr) continue

    if (cStr === pStr) {
      result.push({ key, currentValue: cStr, proposedValue: pStr, status: 'same' })
    } else if (!cStr) {
      result.push({ key, currentValue: '无', proposedValue: pStr, status: 'added' })
    } else if (!pStr) {
      result.push({ key, currentValue: cStr, proposedValue: '已移除', status: 'removed' })
    } else {
      result.push({ key, currentValue: cStr, proposedValue: pStr, status: 'modified' })
    }
  }

  return result
})

function handleReject() {
  expanded.value = false
  emit('reject')
}
</script>

<style scoped>
.patch-compare-card {
  width: 100%;
}

.patch-compare-card__mini {
  padding: 10px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-primary-light);
}

.patch-compare-card__mini-title {
  display: block;
  font-weight: 600;
  font-size: 13px;
}

.patch-compare-card__mini-reason {
  display: block;
  margin-top: 4px;
  color: var(--nb-muted);
  font-size: 12px;
}

.patch-compare-card__mini-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.patch-compare-card__compare {
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: #fff;
  overflow: hidden;
}

.patch-compare-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: var(--nb-border);
  background: var(--nb-surface);
}

.patch-compare-card__header-title {
  font-weight: 600;
  font-size: 13px;
}

.patch-compare-card__reason {
  margin: 0;
  padding: 8px 14px;
  font-size: 12px;
  color: var(--nb-muted);
  border-bottom: 1px solid var(--nb-border-color-light);
}

.patch-compare-card__columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
}

.patch-compare-card__side {
  padding: 10px;
  min-width: 0;
}

.patch-compare-card__side--proposed {
  background: #f8faff;
  border-left: var(--nb-border);
}

.patch-compare-card__side-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--nb-muted);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.patch-compare-card__fields {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.patch-compare-card__field {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  flex-wrap: wrap;
}

.patch-compare-card__field--modified {
  background: #f0f7ff;
}

.patch-compare-card__field--added {
  background: #f0fdf4;
}

.patch-compare-card__field--removed {
  background: #fef2f2;
}

.patch-compare-card__field-label {
  flex-shrink: 0;
  color: var(--nb-muted);
  font-size: 11px;
  min-width: 48px;
}

.patch-compare-card__field-value {
  word-break: break-word;
  min-width: 0;
  flex: 1;
}

.patch-compare-card__field-value--current {
  color: var(--nb-ink);
}

.patch-compare-card__field-value--proposed {
  color: var(--nb-ink);
}

.patch-compare-card__tag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 600;
  flex-shrink: 0;
}

.patch-compare-card__tag--modified {
  background: #dbeafe;
  color: #1d4ed8;
}

.patch-compare-card__tag--added {
  background: #dcfce7;
  color: #16a34a;
}

.patch-compare-card__tag--removed {
  background: #fee2e2;
  color: #dc2626;
}

.patch-compare-card__footer {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  border-top: var(--nb-border);
  background: var(--nb-surface);
}

@media (max-width: 600px) {
  .patch-compare-card__columns {
    grid-template-columns: 1fr;
  }
  .patch-compare-card__side--proposed {
    border-left: none;
    border-top: var(--nb-border);
  }
}
</style>
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
npm run test:unit -- src/components/resume/__tests__/PatchCompareCard.spec.ts
```
Expected: All 5 tests PASS

- [ ] **Step 5: Run type-check**

```bash
npm run type-check
```
Expected: exit 0

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/resume/PatchCompareCard.vue frontend/src/components/resume/__tests__/PatchCompareCard.spec.ts
git commit -m "feat: add PatchCompareCard component with expandable field-by-field comparison"
```

---

### Task 3: Integrate PatchCompareCard into AiChatPanel

**Files:**
- Modify: `src/components/resume/AiChatPanel.vue`
- Test: `src/components/resume/__tests__/AiChatPanel.spec.ts`

Add `sectionDataMap` prop and replace the inline proposal rendering with PatchCompareCard.

- [ ] **Step 1: Update the test to cover the new flow**

Replace `src/components/resume/__tests__/AiChatPanel.spec.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AiChatPanel from '../AiChatPanel.vue'

vi.mock('@/api/resume', () => ({
  getChatHistory: vi.fn().mockResolvedValue({ code: 0, data: [] }),
}))

const textEncoder = new TextEncoder()

function streamFromText(text: string) {
  return new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(textEncoder.encode(text))
      controller.close()
    },
  })
}

describe('AiChatPanel', () => {
  it('renders proposals as PatchCompareCard when receiving resume_patch_proposal', async () => {
    const proposal = JSON.stringify({
      sectionType: 'basic',
      operation: 'replace_section',
      reason: '基本信息可以更规范',
      sectionData: { name: '张三', email: 'zhangsan@xx.com' },
    })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      body: streamFromText(
        `data: 好的，我会帮你优化简历。\n\nevent: resume_patch_proposal\ndata: ${proposal}\n\ndata: [DONE]\n\n`,
      ),
    }))

    const wrapper = mount(AiChatPanel, {
      props: {
        resumeId: 1,
        sectionDataMap: {
          basic: { name: '张三', email: 'a@b.com' },
          education: [],
          work: [],
          project: [],
          skills: {},
          summary: {},
        },
      },
      global: {
        stubs: {
          ElInput: {
            template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup.enter="$emit(\'keyup\', $event)" />',
            props: ['modelValue'],
          },
          NbButton: {
            template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
            props: ['disabled'],
          },
          NbEmptyState: true,
          PatchCompareCard: {
            template: '<div class="patch-compare-card-stub"><slot /></div>',
            props: ['proposal', 'currentData', 'sectionType'],
          },
        },
      },
    })

    await wrapper.find('input').setValue('帮我优化简历')
    await wrapper.find('button').trigger('click')
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('查看对比')
    })

    const assistantBubble = wrapper.find('.ai-chat-panel__bubble--assistant')
    const messageBody = assistantBubble.find('.ai-chat-panel__message-body')
    expect(messageBody.exists()).toBe(true)
    expect(messageBody.find('.ai-chat-panel__content').text()).toContain('好的，我会帮你优化简历。')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
npm run test:unit -- src/components/resume/__tests__/AiChatPanel.spec.ts
```
Expected: FAIL — missing `sectionDataMap` prop or component doesn't render correctly

- [ ] **Step 3: Add `sectionDataMap` prop to AiChatPanel**

Add to the script section:

```ts
const props = defineProps<{
  resumeId: number
  sectionDataMap?: Record<string, unknown>
}>()
```

Add a computed to get current data for a section type:

```ts
function getCurrentData(sectionType: SectionType): Record<string, unknown> | Record<string, unknown>[] {
  const key = sectionType as string
  if (props.sectionDataMap && key in props.sectionDataMap) {
    return props.sectionDataMap[key]
  }
  return {}
}
```

- [ ] **Step 4: Replace proposal rendering with PatchCompareCard**

In the template, replace the current proposal block:

```vue
<div v-if="msg.role === 'assistant' && visibleProposals(msg).length" class="ai-chat-panel__proposals">
  <PatchCompareCard
    v-for="item in visibleProposals(msg)"
    :key="item.index"
    :proposal="item.proposal"
    :current-data="getCurrentData(item.proposal.sectionType)"
    :section-type="item.proposal.sectionType"
    @accept="emit('proposal', item.proposal)"
    @reject="ignoreProposal(msg, item.index)"
  />
</div>
```

Add the import:

```ts
import PatchCompareCard from './PatchCompareCard.vue'
```

Update the emit types to remove the old proposal emit call that's no longer needed... actually keep it as is since PatchCompareCard's @accept emits the same `proposal` event.

Wait, looking at the current code more carefully, `emit('proposal', item.proposal)` is what AiChatPanel emits, and `handlePatchProposal` in ResumeEditPage catches it. The `@accept` on PatchCompareCard should trigger `emit('proposal', item.proposal)`. But the `@reject` should call `ignoreProposal(msg, item.index)` which already exists.

Actually in the template, I'm using `$emit` for PatchCompareCard's events. But `ignoreProposal` is a local method. So:

```vue
<PatchCompareCard
  v-for="item in visibleProposals(msg)"
  :key="item.index"
  :proposal="item.proposal"
  :current-data="getCurrentData(item.proposal.sectionType)"
  :section-type="item.proposal.sectionType"
  @accept="emit('proposal', item.proposal)"
  @reject="ignoreProposal(msg, item.index)"
/>
```

And @reject maps to calling ignoreProposal. In the old code, ignoreProposal adds the index to `ignoredProposalIndexes` which causes the proposal to be filtered out by `visibleProposals(msg)`. This is the same behavior as the old "忽略" button. 

- [ ] **Step 5: Run tests to verify they pass**

```bash
npm run test:unit -- src/components/resume/__tests__/AiChatPanel.spec.ts
```
Expected: PASS

Also run the PatchCompareCard tests:
```bash
npm run test:unit -- src/components/resume/__tests__/PatchCompareCard.spec.ts
```
Expected: PASS

- [ ] **Step 6: Run type-check**

```bash
npm run type-check
```
Expected: exit 0

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/resume/AiChatPanel.vue frontend/src/components/resume/__tests__/AiChatPanel.spec.ts
git commit -m "feat: integrate PatchCompareCard into AiChatPanel"
```

---

### Task 4: Wire up sectionDataMap from ResumeEditPage

**Files:**
- Modify: `src/views/resume/ResumeEditPage.vue`

Pass the existing `sectionDataMap` computed to `AiChatPanel`.

- [ ] **Step 1: Update the AiChatPanel usage in template**

Find the AiChatPanel instance in the template and add the prop:

```vue
<AiChatPanel
  v-if="aiPanelMode === 'chat'"
  :resume-id="resumeId"
  :section-data-map="sectionDataMap"
  @extracted="handleExtracted"
  @proposal="handlePatchProposal"
/>
```

- [ ] **Step 2: Run type-check**

```bash
npm run type-check
```
Expected: exit 0

- [ ] **Step 3: Clean up unused dialog state** (optional cleanup)

The `patchConfirmVisible` and `pendingPatchProposal` refs and the `ResumePatchConfirmDialog` import and template can be removed since they're no longer used. But this is optional - we can keep them for backward compatibility and clean up later.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/resume/ResumeEditPage.vue
git commit -m "feat: pass sectionDataMap to AiChatPanel for PatchCompareCard"
```

---

### Task 5: Full verification

- [ ] **Step 1: Run all frontend tests**

```bash
npm run test:unit
```
Expected: All tests PASS

- [ ] **Step 2: Run type-check**

```bash
npm run type-check
```
Expected: exit 0

- [ ] **Step 3: Run lint**

```bash
npm run lint
```
Expected: exit 0

- [ ] **Step 4: Final status check**

```bash
git status --short
```
Expected: clean working tree (all changes committed)

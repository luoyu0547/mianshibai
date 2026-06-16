<template>
  <div class="patch-compare-card">
    <div v-if="!expanded" class="patch-compare-card__mini">
      <div class="patch-compare-card__mini-summary">
        <span class="patch-compare-card__mini-title">AI 建议修改{{ sectionLabel }}</span>
        <small class="patch-compare-card__mini-reason">{{ proposal.reason || 'AI 建议修改该模块' }}</small>
      </div>
      <div class="patch-compare-card__mini-actions">
        <span class="patch-compare-card__expand-btn" @click="expanded = true"><NbButton size="small" variant="primary">查看对比</NbButton></span>
        <span class="patch-compare-card__ignore-btn" @click="emit('reject')"><NbButton size="small" variant="ghost">忽略</NbButton></span>
      </div>
    </div>

    <div v-else class="patch-compare-card__compare">
      <div class="patch-compare-card__header">
        <div class="patch-compare-card__header-summary">
          <span class="patch-compare-card__header-title">AI 建议修改{{ sectionLabel }}</span>
          <small class="patch-compare-card__header-reason">{{ proposal.reason || 'AI 建议修改该模块' }}</small>
        </div>
        <span class="patch-compare-card__collapse-btn" @click="expanded = false"><NbButton size="small" variant="ghost">收起</NbButton></span>
      </div>

      <div class="patch-compare-card__table">
        <div class="patch-compare-card__table-header">
          <span class="patch-compare-card__table-col patch-compare-card__table-col--label">字段</span>
          <span class="patch-compare-card__table-col patch-compare-card__table-col--current">当前</span>
          <span class="patch-compare-card__table-col patch-compare-card__table-col--proposed">建议</span>
        </div>
        <div
          v-for="field in fields"
          :key="field.key"
          :class="['patch-compare-card__field', `patch-compare-card__field--${field.status}`]"
        >
          <span class="patch-compare-card__field-label">{{ field.label }}</span>
          <span class="patch-compare-card__field-value patch-compare-card__field-value--current">{{ field.currentDisplay }}</span>
          <span class="patch-compare-card__field-value patch-compare-card__field-value--proposed">{{ field.proposedDisplay }}</span>
          <span v-if="field.status === 'modified'" class="patch-compare-card__field-tag tag-modified">修改</span>
          <span v-else-if="field.status === 'added'" class="patch-compare-card__field-tag tag-added">新增</span>
          <span v-else-if="field.status === 'removed'" class="patch-compare-card__field-tag tag-removed">删除</span>
        </div>
      </div>

      <div class="patch-compare-card__actions">
        <span class="patch-compare-card__accept-btn" @click="emit('accept')"><NbButton size="small" variant="primary">同意</NbButton></span>
        <span class="patch-compare-card__reject-btn" @click="emit('reject')"><NbButton size="small" variant="ghost">反对</NbButton></span>
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
  (e: 'accept'): void
  (e: 'reject'): void
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

const fieldLabelMap: Record<string, string> = {
  name: '姓名',
  email: '邮箱',
  phone: '电话',
  targetPosition: '目标职位',
  city: '城市',
  github: 'GitHub',
  blog: '博客',
  avatar: '头像',
  currentStatus: '目前状态',
  expectedLocation: '期望地点',
  expectedSalary: '期望薪资',
  wechat: '微信',
  website: '网站',
  school: '学校',
  major: '专业',
  degree: '学历',
  startDate: '开始时间',
  endDate: '结束时间',
  gpa: 'GPA',
  activities: '在校经历',
  highlights: '亮点',
  company: '公司',
  position: '职位',
  description: '描述',
  role: '角色',
  techStack: '技术栈',
  categories: '技能分类',
  content: '内容',
}

const sectionLabel = computed(() => sectionLabelMap[props.sectionType] || props.sectionType)

const current = computed(() => {
  if (Array.isArray(props.currentData)) return {}
  return props.currentData as Record<string, unknown>
})

const proposed = computed(() => props.proposal.sectionData || {})

function formatValue(val: unknown): string {
  if (val === null || val === undefined) return ''
  if (Array.isArray(val)) return val.join(', ')
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

interface FieldEntry {
  key: string
  label: string
  currentDisplay: string
  proposedDisplay: string
  status: 'same' | 'modified' | 'added' | 'removed'
}

const fields = computed<FieldEntry[]>(() => {
  const allKeys = new Set([...Object.keys(current.value), ...Object.keys(proposed.value)])
  const result: FieldEntry[] = []

  for (const key of allKeys) {
    const curVal = current.value[key]
    const propVal = proposed.value[key]
    const curDisplay = formatValue(curVal)
    const propDisplay = formatValue(propVal)
    const hasCurrent = key in current.value
    const hasProposed = key in proposed.value

    let status: FieldEntry['status']
    if (hasCurrent && !hasProposed) {
      status = 'removed'
    } else if (!hasCurrent && hasProposed) {
      status = 'added'
    } else {
      status = curDisplay === propDisplay ? 'same' : 'modified'
    }

    if (status !== 'same' || curDisplay || propDisplay) {
      result.push({
        key,
        label: fieldLabelMap[key] || key,
        currentDisplay: curDisplay,
        proposedDisplay: propDisplay,
        status,
      })
    }
  }

  return result
})
</script>

<style scoped>
.patch-compare-card {
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
  overflow: hidden;
}

.patch-compare-card__mini {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
}

.patch-compare-card__mini-summary {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.patch-compare-card__mini-title {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  color: var(--nb-ink);
}

.patch-compare-card__mini-reason {
  font-size: 12px;
  color: var(--nb-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.patch-compare-card__mini-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.patch-compare-card__compare {
  padding: 14px;
}

.patch-compare-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.patch-compare-card__header-summary {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.patch-compare-card__header-title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  color: var(--nb-ink);
}

.patch-compare-card__header-reason {
  font-size: 12px;
  color: var(--nb-muted);
}

.patch-compare-card__table {
  margin-bottom: 12px;
}

.patch-compare-card__table-header {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 60px;
  gap: 8px;
  padding: 8px 10px;
  background: var(--nb-muted-surface);
  border-radius: var(--nb-radius-sm);
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 600;
  color: var(--nb-muted);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.patch-compare-card__table-col--label {
  grid-column: 1;
}

.patch-compare-card__table-col--current {
  grid-column: 2;
}

.patch-compare-card__table-col--proposed {
  grid-column: 3;
}

.patch-compare-card__field {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 60px;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  border-bottom: 1px solid var(--nb-border-color-light);
  font-size: 12px;
  line-height: 1.4;
}

.patch-compare-card__field:last-child {
  border-bottom: none;
}

.patch-compare-card__field--modified {
  background: var(--nb-primary-light);
}

.patch-compare-card__field--added {
  background: var(--nb-success-light);
}

.patch-compare-card__field--removed {
  background: var(--nb-danger-light);
}

.patch-compare-card__field-label {
  font-weight: 500;
  color: var(--nb-ink);
  word-break: break-word;
}

.patch-compare-card__field-value {
  word-break: break-word;
  color: var(--nb-muted);
}

.patch-compare-card__field-value--current {
  color: var(--nb-muted);
}

.patch-compare-card__field-value--proposed {
  color: var(--nb-ink);
}

.patch-compare-card__field-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: var(--nb-radius-sm);
  text-align: center;
  white-space: nowrap;
}

.tag-modified {
  background: var(--nb-primary);
  color: #fff;
}

.tag-added {
  background: var(--nb-success);
  color: #fff;
}

.tag-removed {
  background: var(--nb-danger);
  color: #fff;
}

.patch-compare-card__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>

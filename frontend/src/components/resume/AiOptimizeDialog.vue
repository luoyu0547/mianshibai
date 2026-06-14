<!-- src/components/resume/AiOptimizeDialog.vue -->
<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="'AI 优化 — ' + sectionLabel"
    width="860px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="loading" class="optimize-dialog__loading">
      <NbCard variant="ai">
        <NbLoadingBlock title="AI 正在优化中..." :rows="4" />
      </NbCard>
    </div>

    <div v-else-if="error" class="optimize-dialog__error">
      <NbEmptyState
        title="优化失败"
        :description="error"
      >
        <template #action>
          <NbButton variant="primary" @click="doOptimize">重试</NbButton>
        </template>
      </NbEmptyState>
    </div>

    <div v-else class="optimize-dialog__compare">
      <div class="optimize-dialog__side">
        <NbSectionTitle title="原始内容" class="optimize-dialog__side-title" />
        <div class="optimize-dialog__field-list">
          <div
            v-for="row in beforeRows"
            :key="row.key"
            class="optimize-dialog__field optimize-dialog__field--before"
          >
            <span class="optimize-dialog__field-label">{{ row.label }}</span>
            <span class="optimize-dialog__field-value">{{ row.value }}</span>
          </div>
          <p v-if="beforeRows.length === 0" class="optimize-dialog__empty">暂无内容</p>
        </div>
      </div>

      <div class="optimize-dialog__divider">
        <el-icon :size="20"><Right /></el-icon>
      </div>

      <div class="optimize-dialog__side">
        <NbSectionTitle title="优化后" class="optimize-dialog__side-title optimize-dialog__side-title--optimized" />
        <div class="optimize-dialog__field-list">
          <div
            v-for="row in afterRows"
            :key="row.key"
            :class="['optimize-dialog__field', 'optimize-dialog__field--after', { 'optimize-dialog__field--changed': row.changed }]"
          >
            <span class="optimize-dialog__field-label">{{ row.label }}</span>
            <span class="optimize-dialog__field-value">{{ row.value }}</span>
          </div>
          <p v-if="afterRows.length === 0" class="optimize-dialog__empty">暂无内容</p>
        </div>
      </div>
    </div>

    <div v-if="!loading && !error" class="optimize-dialog__json-toggle">
      <el-collapse-transition>
        <pre v-if="showJson" class="optimize-dialog__json">{{ dualJson }}</pre>
      </el-collapse-transition>
      <NbButton variant="ghost" @click="showJson = !showJson">
        {{ showJson ? '隐藏' : '查看' }}原始 JSON
      </NbButton>
    </div>

    <template #footer>
      <NbButton variant="ghost" @click="$emit('update:visible', false)">取消</NbButton>
      <NbButton variant="primary" :disabled="loading || !!error" @click="handleApply">应用优化结果</NbButton>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { aiOptimizeSection } from '@/api/resume'
import type { SectionType } from '@/types/resume'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'

const props = defineProps<{
  visible: boolean
  resumeId: number
  sectionType: SectionType
  sectionData: Record<string, unknown> | Record<string, unknown>[]
  sectionLabel: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'applied', sectionType: SectionType, data: Record<string, unknown> | Record<string, unknown>[]): void
}>()

const loading = ref(false)
const error = ref('')
const optimizedData = ref<Record<string, unknown> | Record<string, unknown>[]>({})
const showJson = ref(false)

const FIELD_LABELS: Record<string, string> = {
  name: '姓名',
  email: '邮箱',
  phone: '手机号',
  targetPosition: '目标岗位',
  city: '城市',
  avatar: '头像',
  currentStatus: '当前状态',
  expectedLocation: '期望工作地',
  expectedSalary: '期望薪资',
  wechat: '微信',
  website: '个人网站',
  github: 'GitHub',
  blog: '博客',
  school: '学校',
  major: '专业',
  degree: '学历',
  gpa: 'GPA',
  startDate: '开始时间',
  endDate: '结束时间',
  highlights: '亮点标签',
  company: '公司',
  position: '职位',
  description: '描述',
  role: '角色',
  techStack: '技术栈',
  content: '内容',
  categories: '技能分类',
}

interface FieldRow {
  key: string
  label: string
  value: string
  changed?: boolean
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return ''
  if (Array.isArray(value)) {
    if (value.length === 0) return ''
    if (value.every((v) => typeof v !== 'object' || v === null)) {
      return value.join('、')
    }
    return value.map((v) => formatObject(v as Record<string, unknown>)).join('； ')
  }
  if (typeof value === 'object') return formatObject(value as Record<string, unknown>)
  return String(value)
}

function formatObject(obj: Record<string, unknown>): string {
  return Object.entries(obj)
    .filter(([, v]) => v !== '' && v !== null && v !== undefined && !(Array.isArray(v) && v.length === 0))
    .map(([k, v]) => {
      const label = FIELD_LABELS[k] || k
      const val = formatValue(v)
      return val ? `${label}: ${val}` : label
    })
    .join('，')
}

function flattenData(data: Record<string, unknown> | unknown[]): FieldRow[] {
  const rows: FieldRow[] = []
  if (Array.isArray(data)) {
    data.forEach((item, idx) => {
      if (item && typeof item === 'object') {
        rows.push({ key: `__hdr_${idx}`, label: `# ${idx + 1}`, value: '' })
        const objRows = flattenObject(item as Record<string, unknown>)
        objRows.forEach((r) => rows.push({ ...r, key: `${idx}-${r.key}` }))
      }
    })
    return rows
  }
  return flattenObject(data)
}

function flattenObject(obj: Record<string, unknown>): FieldRow[] {
  const rows: FieldRow[] = []
  for (const [key, value] of Object.entries(obj)) {
    const label = FIELD_LABELS[key] || key
    const display = formatValue(value)
    if (display) {
      rows.push({ key, label, value: display })
    }
  }
  return rows
}

const beforeRows = computed<FieldRow[]>(() => flattenData(props.sectionData))
const afterRows = computed<FieldRow[]>(() => {
  const after = flattenData(optimizedData.value)
  const beforeMap = new Map(beforeRows.value.map((r) => [r.key, r.value]))
  return after.map((r) => ({
    ...r,
    changed: beforeMap.get(r.key) !== r.value,
  }))
})

const dualJson = computed(() => {
  return '--- 原始 ---\n' + safeStringify(props.sectionData) + '\n\n--- 优化后 ---\n' + safeStringify(optimizedData.value)
})

function safeStringify(data: unknown): string {
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    showJson.value = false
    doOptimize()
  }
})

async function doOptimize() {
  loading.value = true
  error.value = ''
  try {
    const res = await aiOptimizeSection(props.resumeId, {
      sectionId: 0,
      sectionType: props.sectionType,
      sectionData: props.sectionData,
    })
    if (res.code === 0 && res.data) {
      optimizedData.value = res.data
    } else {
      error.value = '优化失败，请重试'
    }
  } catch {
    error.value = '网络错误，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

function handleApply() {
  emit('applied', props.sectionType, optimizedData.value)
  emit('update:visible', false)
  ElMessage.success('已应用优化结果')
}
</script>

<style scoped>
.optimize-dialog__loading {
  padding: 8px 0;
}

.optimize-dialog__error {
  padding: 8px 0;
}

.optimize-dialog__compare {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.optimize-dialog__side {
  flex: 1;
  min-width: 0;
}

.optimize-dialog__side-title {
  margin-bottom: 4px;
}

.optimize-dialog__side-title--optimized :deep(.nb-section-title__heading) {
  color: var(--nb-primary);
}

.optimize-dialog__field-list {
  max-height: 380px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.optimize-dialog__field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
}

.optimize-dialog__field--after {
  background: var(--nb-surface);
}

.optimize-dialog__field--changed {
  border-color: var(--nb-primary);
  box-shadow: var(--nb-shadow-xs);
  background: rgba(108, 92, 231, 0.04);
}

.optimize-dialog__field-label {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 600;
  color: var(--nb-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.optimize-dialog__field-value {
  font-size: 13px;
  line-height: 1.5;
  color: var(--nb-ink);
  white-space: pre-wrap;
  word-break: break-word;
}

.optimize-dialog__field--changed .optimize-dialog__field-value {
  font-weight: 500;
}

.optimize-dialog__empty {
  text-align: center;
  color: var(--nb-muted);
  font-size: 13px;
  padding: 24px 0;
}

.optimize-dialog__divider {
  display: flex;
  align-items: center;
  padding-top: 48px;
  color: var(--nb-primary);
  flex-shrink: 0;
}

.optimize-dialog__json-toggle {
  margin-top: 16px;
  padding-top: 16px;
  border-top: var(--nb-border);
}

.optimize-dialog__json {
  margin: 0 0 12px;
  padding: 12px;
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  max-height: 200px;
  overflow-y: auto;
}
</style>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { optimizeWholeResume } from '@/api/resume'
import type { ResumeWholeOptimizeVO, SectionVO, SectionType } from '@/types/resume'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'

const props = defineProps<{
  resumeId: number
  originalSections: SectionVO[]
}>()

const emit = defineEmits<{
  apply: [sections: SectionVO[]]
}>()

const visible = ref(false)
const loading = ref(false)
const result = ref<ResumeWholeOptimizeVO | null>(null)
const activeTab = ref<SectionType>('work')

const sectionLabels: Record<SectionType, string> = {
  basic: '基本信息', education: '教育经历', work: '工作经历',
  project: '项目经历', skills: '技能标签', summary: '自我评价',
}

const tabTypes: SectionType[] = ['basic', 'education', 'work', 'project', 'skills', 'summary']

const sectionTypes = computed(() =>
  tabTypes.filter(t => hasSection(t))
)

function hasSection(type: SectionType) {
  return result.value?.optimizedSections.some(s => s.sectionType === type)
}

function getOriginal(type: SectionType): Record<string, unknown> {
  return (props.originalSections.find(s => s.sectionType === type)?.sectionData || {}) as Record<string, unknown>
}

function getOptimized(type: SectionType): Record<string, unknown> {
  return (result.value?.optimizedSections.find(s => s.sectionType === type)?.sectionData || {}) as Record<string, unknown>
}

function sectionKeys(type: SectionType, data: Record<string, unknown>): string[] {
  const obj = data || {}
  if (type === 'education') return ['school', 'major', 'degree', 'startDate', 'endDate', 'gpa', 'activities']
  if (type === 'work') return ['company', 'position', 'startDate', 'endDate', 'description']
  if (type === 'project') return ['name', 'role', 'startDate', 'endDate', 'description', 'techStack']
  if (type === 'basic') return ['name', 'email', 'phone', 'targetPosition', 'city', 'currentStatus']
  if (type === 'skills') return [] // handled separately
  if (type === 'summary') return ['content']
  return Object.keys(obj)
}

function fmtVal(v: unknown): string {
  if (v === null || v === undefined) return '—'
  if (Array.isArray(v)) return v.join('、')
  return String(v)
}

function formatDiff(orig: unknown, opt: unknown): { text: string; changed: boolean } {
  const o = fmtVal(orig)
  const n = fmtVal(opt)
  return { text: n, changed: o !== n }
}

async function handleOptimize() {
  loading.value = true
  try {
    const res = await optimizeWholeResume(props.resumeId, {
      resumeId: props.resumeId,
    })
    result.value = res.data
    const types = sectionTypes.value
    if (types.length > 0) {
      activeTab.value = types[0]!
    }
  } finally {
    loading.value = false
  }
}

function handleApply() {
  if (result.value) {
    emit('apply', result.value.optimizedSections)
    visible.value = false
    result.value = null
  }
}

function open() {
  visible.value = true
  handleOptimize()
}

function close() {
  visible.value = false
  result.value = null
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="整份简历 AI 优化" width="820px" @close="close">
    <div v-if="loading" class="wo-loading">
      <NbLoadingBlock title="AI 正在逐模块深度优化您的简历..." :rows="4" />
    </div>

    <div v-else-if="result" class="wo-body">
      <div class="wo-scores">
        <div class="wo-score-card">
          <span>优化前</span>
          <strong>{{ result.beforeScore }}</strong>
        </div>
        <div class="wo-arrow">→</div>
        <div class="wo-score-card wo-score-card--after">
          <span>优化后</span>
          <strong>{{ result.estimatedAfterScore }}</strong>
        </div>
      </div>

      <div class="wo-diff">
        <el-tabs v-model="activeTab" type="card">
          <el-tab-pane v-for="type in sectionTypes" :key="type" :label="sectionLabels[type]" :name="type">
            <div v-if="type === 'skills'" class="wo-diff-pane">
              <div class="wo-diff-field">
                <div class="wo-diff-label">原技能</div>
                <div class="wo-diff-orig">
                  <template v-for="cat in (getOriginal('skills') as any)?.categories" :key="cat.name">
                    <strong>{{ cat.name }}：</strong>{{ (cat.items as string[])?.join('、') }}
                    <br />
                  </template>
                </div>
              </div>
              <div class="wo-diff-field">
                <div class="wo-diff-label">优化后</div>
                <div class="wo-diff-opt">
                  <template v-for="cat in (getOptimized('skills') as any)?.categories" :key="cat.name">
                    <strong>{{ cat.name }}：</strong>{{ (cat.items as string[])?.join('、') }}
                    <br />
                  </template>
                </div>
              </div>
            </div>
            <div v-else class="wo-diff-pane">
              <div v-for="key in sectionKeys(type, getOriginal(type) || getOptimized(type))" :key="key" class="wo-diff-field">
                <div class="wo-diff-label">{{ key }}</div>
                <div class="wo-diff-row">
                  <span class="wo-diff-val wo-diff-orig" v-html="fmtVal(getOriginal(type)?.[key])"></span>
                  <span class="wo-diff-val" :class="{ 'wo-diff-changed': formatDiff(getOriginal(type)?.[key], getOptimized(type)?.[key]).changed }" v-html="fmtVal(getOptimized(type)?.[key])"></span>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-if="result.globalSuggestions.length" class="wo-suggestions">
        <h3>全局优化建议</h3>
        <ol>
          <li v-for="(s, i) in result.globalSuggestions" :key="i">{{ s }}</li>
        </ol>
      </div>
    </div>

    <div v-else class="wo-empty">
      <p>暂无优化结果</p>
      <NbButton variant="primary" @click="handleOptimize">重新分析</NbButton>
    </div>

    <template #footer>
      <NbButton variant="ghost" @click="visible = false">取消</NbButton>
      <NbButton variant="primary" :disabled="loading || !result" @click="handleApply">应用优化（全部模块）</NbButton>
    </template>
  </el-dialog>
</template>

<style scoped>
.wo-loading { padding: 16px 0; }
.wo-body { display: flex; flex-direction: column; gap: 18px; }
.wo-scores { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 14px; }
.wo-score-card { padding: 16px 18px; border: 1px solid var(--nb-border-color); border-radius: var(--nb-radius-lg); background: #fff; text-align: center; }
.wo-score-card span { display: block; color: var(--nb-muted); font-size: 13px; }
.wo-score-card strong { display: block; margin-top: 4px; font-family: var(--font-heading); font-size: 32px; line-height: 1; }
.wo-score-card--after { border-color: rgba(63,109,246,0.35); background: #edf3ff; }
.wo-score-card--after strong { color: #2f63ef; }
.wo-arrow { font-family: var(--font-heading); font-size: 22px; color: var(--nb-muted-light); }

.wo-diff { border: 1px solid var(--nb-border-color); border-radius: var(--nb-radius-lg); overflow: hidden; }
.wo-diff :deep(.el-tabs__header) { margin: 0; background: #fafbfc; border-bottom: 1px solid var(--nb-border-color); }
.wo-diff :deep(.el-tabs__nav) { border: none; }
.wo-diff-pane { padding: 14px 16px; max-height: 360px; overflow-y: auto; }
.wo-diff-field { margin-bottom: 12px; }
.wo-diff-label { font-family: var(--font-heading); font-size: 12px; font-weight: 600; color: var(--nb-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
.wo-diff-row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.wo-diff-val { font-size: 12.5px; line-height: 1.6; padding: 6px 10px; border-radius: 6px; background: #f9fafb; color: var(--nb-muted); word-break: break-word; }
.wo-diff-orig { background: #fff3f3; color: #9b4449; }
.wo-diff-changed { background: #edf7ed; color: #2e7d32; font-weight: 500; border: 1px solid #c8e6c9; }

.wo-suggestions { padding: 16px 18px; border-radius: var(--nb-radius-lg); background: #f8fafc; border: 1px solid var(--nb-border-color-light); }
.wo-suggestions h3 { margin: 0 0 10px; font-family: var(--font-heading); font-size: 15px; }
.wo-suggestions ol { margin: 0; padding-left: 20px; color: var(--nb-ink); line-height: 1.8; font-size: 13px; }

.wo-empty { color: var(--nb-muted); text-align: center; padding: 32px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
</style>

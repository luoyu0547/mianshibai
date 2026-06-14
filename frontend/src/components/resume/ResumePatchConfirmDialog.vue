<template>
  <el-dialog
    :model-value="visible"
    title="确认 AI 修改"
    width="860px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="patch-dialog__summary">
      <strong>{{ sectionLabel }}</strong>
      <span>{{ proposal?.reason || 'AI 建议修改该模块' }}</span>
    </div>

    <div class="patch-dialog__compare">
      <div class="patch-dialog__side">
        <NbSectionTitle title="当前内容" />
        <pre>{{ currentJson }}</pre>
      </div>
      <div class="patch-dialog__side patch-dialog__side--after">
        <NbSectionTitle title="AI 建议" />
        <pre>{{ proposedJson }}</pre>
      </div>
    </div>

    <template #footer>
      <NbButton variant="ghost" @click="$emit('update:visible', false)">取消</NbButton>
      <NbButton variant="primary" :disabled="!proposal" @click="handleApply">应用到简历</NbButton>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumePatchProposal, SectionType } from '@/types/resume'
import NbButton from '@/components/NbButton.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'

const props = defineProps<{
  visible: boolean
  proposal: ResumePatchProposal | null
  currentData: Record<string, unknown> | Record<string, unknown>[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'apply', proposal: ResumePatchProposal): void
}>()

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

const sectionLabel = computed(() => {
  const type = props.proposal?.sectionType
  return type ? sectionLabelMap[type] : '简历模块'
})

const currentJson = computed(() => JSON.stringify(props.currentData, null, 2))
const proposedJson = computed(() => JSON.stringify(props.proposal?.sectionData || {}, null, 2))

function handleApply() {
  if (!props.proposal) return
  emit('apply', props.proposal)
  emit('update:visible', false)
}
</script>

<style scoped>
.patch-dialog__summary {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  margin-bottom: 14px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-primary-light);
}

.patch-dialog__compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.patch-dialog__side {
  min-width: 0;
  padding: 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
}

.patch-dialog__side--after {
  box-shadow: var(--nb-shadow-xs);
}

pre {
  max-height: 420px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .patch-dialog__compare {
    grid-template-columns: 1fr;
  }
}
</style>

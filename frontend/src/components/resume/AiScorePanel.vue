<!-- src/components/resume/AiScorePanel.vue -->
<template>
  <div class="ai-score-panel">
    <NbCard v-if="!scoreData" variant="ai" class="ai-score-panel__trigger-card">
      <NbEmptyState variant="ai" title="AI 简历评分" description="获取 AI 对你简历完整度、专业性与岗位匹配的智能评估">
        <template #action>
          <NbButton variant="accent" :loading="loading" @click="handleScore">开始评分</NbButton>
        </template>
      </NbEmptyState>
    </NbCard>

    <template v-else>
      <NbCard variant="ai" class="ai-score-panel__hero">
        <div class="ai-score-panel__hero-row">
          <div class="ai-score-panel__hero-score">
            <span class="ai-score-panel__score-value">{{ scoreData.score }}</span>
            <span class="ai-score-panel__score-unit">/ 100</span>
          </div>
          <NbStatusBadge
            :label="scoreTier.label"
            :variant="scoreTier.variant"
          />
        </div>
        <span class="ai-score-panel__score-label">综合评分</span>
      </NbCard>

      <div class="ai-score-panel__dimensions">
        <NbCard
          v-for="dim in dimensionList"
          :key="dim.key"
          compact
          class="ai-score-panel__dimension"
        >
          <div class="ai-score-panel__dim-header">
            <span class="ai-score-panel__dim-label">{{ dim.label }}</span>
            <span class="ai-score-panel__dim-score">{{ dim.value }}<small> / 100</small></span>
          </div>
          <el-progress
            :percentage="dim.value"
            :stroke-width="10"
            :show-text="false"
            :color="dim.color"
          />
          <p v-if="dim.comment" class="ai-score-panel__dim-comment">{{ dim.comment }}</p>
        </NbCard>
      </div>

      <NbCard v-if="scoreData.suggestions?.length" variant="warning" class="ai-score-panel__suggestions">
        <NbSectionTitle title="优化建议" description="针对薄弱维度的改进方向" />
        <div class="ai-score-panel__suggestion-list">
          <div
            v-for="(suggestion, index) in scoreData.suggestions"
            :key="index"
            class="ai-score-panel__suggestion"
          >
            <span class="ai-score-panel__suggestion-num">{{ index + 1 }}</span>
            <span class="ai-score-panel__suggestion-text">{{ suggestion }}</span>
          </div>
        </div>
      </NbCard>

      <NbButton variant="ghost" block :loading="loading" @click="handleScore">重新评分</NbButton>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { aiScoreResume } from '@/api/resume'
import type { AiScoreVO } from '@/types/resume'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'

const props = defineProps<{
  resumeId: number
}>()

const loading = ref(false)
const scoreData = ref<AiScoreVO | null>(null)

const scoreTier = computed(() => {
  const score = scoreData.value?.score ?? 0
  if (score >= 80) return { label: '优秀', variant: 'success' as const }
  if (score >= 60) return { label: '良好', variant: 'warning' as const }
  return { label: '待改进', variant: 'danger' as const }
})

const dimensionList = computed(() => {
  const d = scoreData.value?.dimensions
  if (!d) return []
  return [
    { key: 'completeness', label: '完整性', value: d.completeness, comment: d.completenessComment, color: 'var(--nb-primary)' },
    { key: 'professionalism', label: '专业性', value: d.professionalism, comment: d.professionalismComment, color: 'var(--nb-secondary)' },
    { key: 'matching', label: '岗位匹配度', value: d.matching, comment: d.matchingComment, color: 'var(--nb-accent)' },
  ]
})

async function handleScore() {
  loading.value = true
  try {
    const res = await aiScoreResume(props.resumeId)
    if (res.code === 0) {
      scoreData.value = res.data
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-score-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  height: 100%;
  overflow-y: auto;
}

.ai-score-panel__trigger-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-score-panel__hero {
  text-align: center;
}

.ai-score-panel__hero-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.ai-score-panel__hero-score {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.ai-score-panel__score-value {
  font-family: var(--font-heading);
  font-size: 48px;
  font-weight: 700;
  color: var(--nb-primary);
  line-height: 1;
}

.ai-score-panel__score-unit {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  color: var(--nb-muted);
}

.ai-score-panel__score-label {
  display: block;
  font-size: 13px;
  color: var(--nb-muted);
  margin-top: 4px;
}

.ai-score-panel__dimensions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-score-panel__dim-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.ai-score-panel__dim-label {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
}

.ai-score-panel__dim-score {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 16px;
  color: var(--nb-primary);
}

.ai-score-panel__dim-score small {
  font-size: 11px;
  font-weight: 500;
  color: var(--nb-muted);
}

.ai-score-panel__dim-comment {
  font-size: 12px;
  color: var(--nb-muted);
  margin: 6px 0 0;
  line-height: 1.5;
}

.ai-score-panel__suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.ai-score-panel__suggestion {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: var(--nb-surface);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
}

.ai-score-panel__suggestion-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  background: var(--nb-warning);
  border: var(--nb-border);
  border-radius: var(--nb-radius-sm);
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 700;
}

.ai-score-panel__suggestion-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--nb-ink);
}
</style>

<!-- src/components/resume/AiScorePanel.vue -->
<template>
  <div class="ai-score-panel">
    <div v-if="!scoreData" class="ai-score-panel__trigger">
      <el-button type="primary" :loading="loading" @click="handleScore">
        AI 评分
      </el-button>
    </div>

    <div v-else class="ai-score-panel__result">
      <div class="ai-score-panel__total">
        <span class="ai-score-panel__score">{{ scoreData.score }}</span>
        <span class="ai-score-panel__label">综合评分</span>
      </div>

      <div class="ai-score-panel__dimensions">
        <div class="ai-score-panel__dimension">
          <div class="ai-score-panel__dimension-header">
            <span>完整性</span>
            <span class="ai-score-panel__dimension-score">{{ scoreData.dimensions.completeness }}</span>
          </div>
          <el-progress
            :percentage="scoreData.dimensions.completeness"
            :stroke-width="10"
            :show-text="false"
            color="var(--nb-primary)"
          />
          <p class="ai-score-panel__comment">{{ scoreData.dimensions.completenessComment }}</p>
        </div>

        <div class="ai-score-panel__dimension">
          <div class="ai-score-panel__dimension-header">
            <span>专业性</span>
            <span class="ai-score-panel__dimension-score">{{ scoreData.dimensions.professionalism }}</span>
          </div>
          <el-progress
            :percentage="scoreData.dimensions.professionalism"
            :stroke-width="10"
            :show-text="false"
            color="var(--nb-secondary)"
          />
          <p class="ai-score-panel__comment">{{ scoreData.dimensions.professionalismComment }}</p>
        </div>

        <div class="ai-score-panel__dimension">
          <div class="ai-score-panel__dimension-header">
            <span>岗位匹配度</span>
            <span class="ai-score-panel__dimension-score">{{ scoreData.dimensions.matching }}</span>
          </div>
          <el-progress
            :percentage="scoreData.dimensions.matching"
            :stroke-width="10"
            :show-text="false"
            color="var(--nb-accent)"
          />
          <p class="ai-score-panel__comment">{{ scoreData.dimensions.matchingComment }}</p>
        </div>
      </div>

      <div v-if="scoreData.suggestions?.length" class="ai-score-panel__suggestions">
        <h4 class="ai-score-panel__suggestions-title">优化建议</h4>
        <el-alert
          v-for="(suggestion, index) in scoreData.suggestions"
          :key="index"
          :title="suggestion"
          type="warning"
          :closable="false"
          show-icon
          class="ai-score-panel__suggestion"
        />
      </div>

      <el-button class="ai-score-panel__rescore" :loading="loading" @click="handleScore">
        重新评分
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { aiScoreResume } from '@/api/resume'
import type { AiScoreVO } from '@/types/resume'

const props = defineProps<{
  resumeId: number
}>()

const loading = ref(false)
const scoreData = ref<AiScoreVO | null>(null)

async function handleScore() {
  loading.value = true
  try {
    const res = await aiScoreResume(props.resumeId)
    if (res.data.code === 0) {
      scoreData.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-score-panel {
  padding: 16px;
}

.ai-score-panel__trigger {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.ai-score-panel__result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-score-panel__total {
  text-align: center;
  padding: 16px 0;
}

.ai-score-panel__score {
  display: block;
  font-family: var(--font-heading);
  font-size: 48px;
  font-weight: 700;
  color: var(--nb-primary);
  line-height: 1;
}

.ai-score-panel__label {
  font-size: 14px;
  color: var(--nb-muted);
  margin-top: 4px;
}

.ai-score-panel__dimensions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-score-panel__dimension-header {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
}

.ai-score-panel__dimension-score {
  font-family: var(--font-heading);
  font-weight: 600;
  color: var(--nb-primary);
}

.ai-score-panel__comment {
  font-size: 12px;
  color: var(--nb-muted);
  margin: 4px 0 0;
  line-height: 1.4;
}

.ai-score-panel__suggestions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-score-panel__suggestions-title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  margin: 0;
}

.ai-score-panel__suggestion {
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius);
}

.ai-score-panel__rescore {
  width: 100%;
}
</style>

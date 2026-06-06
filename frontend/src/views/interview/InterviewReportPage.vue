<template>
  <MainLayout>
    <div class="interview-report-page">
      <div class="interview-report-page__header">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="interview-report-page__title">面试报告</h1>
      </div>

      <div v-if="interviewStore.loading" class="interview-report-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="report">
        <div class="interview-report-page__score-section">
          <NbCard class="interview-report-page__total-score">
            <div class="interview-report-page__score-label">综合评分</div>
            <div class="interview-report-page__score-value">{{ report.totalScore }}</div>
            <div class="interview-report-page__score-hint">满分 100</div>
          </NbCard>
        </div>

        <div class="interview-report-page__dimensions">
          <NbCard v-for="dim in dimensions" :key="dim.key" class="interview-report-page__dim-card">
            <div class="interview-report-page__dim-header">
              <span class="interview-report-page__dim-label">{{ dim.label }}</span>
              <span class="interview-report-page__dim-score">{{ report[dim.key] }}</span>
            </div>
            <el-progress
              :percentage="report[dim.key]"
              :stroke-width="12"
              :color="dim.color"
              :show-text="false"
            />
          </NbCard>
        </div>

        <NbCard class="interview-report-page__section">
          <h3 class="interview-report-page__section-title">AI 总结</h3>
          <p class="interview-report-page__summary-text">{{ report.summary }}</p>
        </NbCard>

        <NbCard v-if="report.suggestions.length > 0" class="interview-report-page__section">
          <h3 class="interview-report-page__section-title">改进建议</h3>
          <ul class="interview-report-page__suggestions">
            <li v-for="(suggestion, idx) in report.suggestions" :key="idx">
              {{ suggestion }}
            </li>
          </ul>
        </NbCard>

        <NbCard class="interview-report-page__section">
          <h3 class="interview-report-page__section-title">答题回顾</h3>
          <el-collapse>
            <el-collapse-item
              v-for="turn in report.turns"
              :key="turn.id"
              :title="`第 ${turn.questionNo} 题${turn.turnType === 'follow_up' ? '（追问）' : ''}`"
              :name="turn.id"
            >
              <div class="interview-report-page__turn">
                <div class="interview-report-page__turn-block">
                  <div class="interview-report-page__turn-label">问题</div>
                  <div class="interview-report-page__turn-text">{{ turn.questionText }}</div>
                </div>
                <div class="interview-report-page__turn-block">
                  <div class="interview-report-page__turn-label">回答</div>
                  <div class="interview-report-page__turn-text">{{ turn.answerText || '未回答' }}</div>
                </div>
                <div v-if="turn.aiFeedback" class="interview-report-page__turn-block">
                  <div class="interview-report-page__turn-label">AI 评价</div>
                  <div class="interview-report-page__turn-text">{{ turn.aiFeedback }}</div>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </NbCard>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useInterviewStore } from '@/stores/interview'

const route = useRoute()
const router = useRouter()
const interviewStore = useInterviewStore()

const sessionId = computed(() => Number(route.params.id) || 0)
const report = computed(() => interviewStore.currentReport)

const dimensions = [
  { key: 'accuracyScore' as const, label: '技术准确性', color: '#6C5CE7' },
  { key: 'clarityScore' as const, label: '表达清晰度', color: '#00CEC9' },
  { key: 'depthScore' as const, label: '项目深度', color: '#FD79A8' },
  { key: 'matchingScore' as const, label: '岗位匹配度', color: '#00B894' },
]

onMounted(() => {
  interviewStore.fetchReport(sessionId.value)
})
</script>

<style scoped>
.interview-report-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.interview-report-page__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.interview-report-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.interview-report-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.interview-report-page__score-section {
  display: flex;
  justify-content: center;
}

.interview-report-page__total-score {
  text-align: center;
  padding: 32px 48px;
  min-width: 200px;
}

.interview-report-page__score-label {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
  color: var(--nb-muted);
  margin-bottom: 8px;
}

.interview-report-page__score-value {
  font-family: var(--font-heading);
  font-size: 64px;
  font-weight: 700;
  color: var(--nb-primary);
  line-height: 1;
}

.interview-report-page__score-hint {
  font-size: 13px;
  color: var(--nb-muted);
  margin-top: 8px;
}

.interview-report-page__dimensions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.interview-report-page__dim-card {
  padding: 16px 20px;
}

.interview-report-page__dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.interview-report-page__dim-label {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
}

.interview-report-page__dim-score {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
  color: var(--nb-primary);
}

.interview-report-page__section {
  padding: 24px;
}

.interview-report-page__section-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px;
}

.interview-report-page__summary-text {
  font-size: 15px;
  line-height: 1.8;
  color: var(--nb-text);
  margin: 0;
}

.interview-report-page__suggestions {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.interview-report-page__suggestions li {
  padding: 12px 16px;
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  font-size: 14px;
  line-height: 1.6;
}

.interview-report-page__turn {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.interview-report-page__turn-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.interview-report-page__turn-label {
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 600;
  color: var(--nb-muted);
}

.interview-report-page__turn-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--nb-text);
}
</style>

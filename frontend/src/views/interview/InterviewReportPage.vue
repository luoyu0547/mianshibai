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

        <NbCard class="interview-report-page__section">
          <div class="interview-report-page__section-header">
            <h3 class="interview-report-page__section-title">复盘增强</h3>
            <el-tag v-if="enhancement" :type="enhancement.status === 'completed' ? 'success' : enhancement.status === 'failed' ? 'danger' : 'warning'" size="small">
              {{ enhancement.status === 'pending' || enhancement.status === 'running' ? '生成中' : enhancement.status === 'completed' ? '已完成' : '失败' }}
            </el-tag>
          </div>
          <p v-if="!enhancement || enhancement.status === 'pending' || enhancement.status === 'running'" class="interview-report-page__summary-text">
            AI 正在生成逐题复盘和优秀回答，稍后会自动刷新。
          </p>
          <template v-else-if="enhancement.status === 'failed'">
            <p class="interview-report-page__summary-text">{{ enhancement.errorMessage || '复盘增强生成失败' }}</p>
            <el-button type="primary" size="small" @click="retryEnhancement" style="margin-top: 12px">重新生成</el-button>
          </template>
          <template v-else>
            <p class="interview-report-page__summary-text">{{ enhancement.summary }}</p>
            <div v-if="enhancement.skillGaps.length > 0" class="interview-report-page__tags">
              <el-tag v-for="gap in enhancement.skillGaps" :key="gap.name" type="warning" size="small" style="margin: 2px 4px 2px 0">
                {{ gap.name }} · {{ gap.severity }}
              </el-tag>
            </div>
            <div v-if="enhancement.actionItems.length > 0" style="margin-top: 16px">
              <div class="interview-report-page__turn-label" style="margin-bottom: 8px">下一步行动</div>
              <ul class="interview-report-page__suggestions">
                <li v-for="(item, idx) in enhancement.actionItems" :key="idx">{{ item }}</li>
              </ul>
            </div>
          </template>
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
                <template v-if="enhancement?.status === 'completed' && findTurnReview(turn.id)">
                  <div class="interview-report-page__turn-block">
                    <div class="interview-report-page__turn-label">问题诊断</div>
                    <div class="interview-report-page__turn-text">{{ findTurnReview(turn.id)?.diagnosis }}</div>
                  </div>
                  <div class="interview-report-page__turn-block">
                    <div class="interview-report-page__turn-label">优秀回答</div>
                    <div class="interview-report-page__turn-text">{{ findTurnReview(turn.id)?.excellentAnswer }}</div>
                  </div>
                  <div class="interview-report-page__turn-block">
                    <div class="interview-report-page__turn-label">我的回答改进版</div>
                    <div class="interview-report-page__turn-text">{{ findTurnReview(turn.id)?.improvedAnswer }}</div>
                  </div>
                  <div v-if="findTurnReview(turn.id)?.knowledgePoints?.length" class="interview-report-page__turn-block">
                    <div class="interview-report-page__turn-label">考察知识点</div>
                    <div style="display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px">
                      <el-tag v-for="kp in findTurnReview(turn.id)?.knowledgePoints" :key="kp" size="small">{{ kp }}</el-tag>
                    </div>
                  </div>
                </template>
              </div>
            </el-collapse-item>
          </el-collapse>
        </NbCard>

        <NbCard v-if="previousCompletedSessionId" class="interview-report-page__section">
          <h3 class="interview-report-page__section-title">报告对比</h3>
          <el-button type="primary" size="small" @click="compareWithPrevious">与上次报告对比</el-button>
          <div v-if="comparison" style="margin-top: 16px">
            <div class="interview-report-page__compare-scores">
              <span>{{ comparison.baseTotalScore }} 分</span>
              <span style="margin: 0 8px">→</span>
              <span :style="{ color: comparison.totalDelta >= 0 ? '#00B894' : '#E17055' }">
                {{ comparison.targetTotalScore }} 分（{{ comparison.totalDelta >= 0 ? '+' : '' }}{{ comparison.totalDelta }}）
              </span>
            </div>
            <div style="margin-top: 12px">
              <div v-for="dim in comparison.dimensions" :key="dim.key" class="interview-report-page__compare-dim">
                <span>{{ dim.label }}</span>
                <span>{{ dim.baseScore }} → {{ dim.targetScore }}</span>
                <span :style="{ color: dim.delta >= 0 ? '#00B894' : '#E17055' }">
                  {{ dim.delta >= 0 ? '+' : '' }}{{ dim.delta }}
                </span>
              </div>
            </div>
          </div>
        </NbCard>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useInterviewStore } from '@/stores/interview'

const route = useRoute()
const router = useRouter()
const interviewStore = useInterviewStore()

const sessionId = computed(() => Number(route.params.id) || 0)
const report = computed(() => interviewStore.currentReport)
const enhancement = computed(() => interviewStore.currentEnhancement)
const comparison = computed(() => interviewStore.currentComparison)

const dimensions = [
  { key: 'accuracyScore' as const, label: '技术准确性', color: '#6C5CE7' },
  { key: 'clarityScore' as const, label: '表达清晰度', color: '#00CEC9' },
  { key: 'depthScore' as const, label: '项目深度', color: '#FD79A8' },
  { key: 'matchingScore' as const, label: '岗位匹配度', color: '#00B894' },
]

let enhancementTimer: number | undefined

function shouldPollEnhancement() {
  return enhancement.value?.status === 'pending' || enhancement.value?.status === 'running'
}

async function loadEnhancement() {
  await interviewStore.fetchReportEnhancement(sessionId.value)
  if (shouldPollEnhancement() && enhancementTimer === undefined) {
    enhancementTimer = window.setInterval(async () => {
      await interviewStore.fetchReportEnhancement(sessionId.value)
      if (!shouldPollEnhancement() && enhancementTimer !== undefined) {
        window.clearInterval(enhancementTimer)
        enhancementTimer = undefined
      }
    }, 5000)
  }
}

async function retryEnhancement() {
  await interviewStore.retryReportEnhancement(sessionId.value)
  ElMessage.success('已重新提交复盘增强任务')
  await loadEnhancement()
}

const previousCompletedSessionId = computed(() => {
  return interviewStore.sessions
    .filter((item) => item.status === 'completed' && item.id !== sessionId.value)
    .sort((a, b) => b.id - a.id)[0]?.id ?? null
})

async function compareWithPrevious() {
  if (!previousCompletedSessionId.value) return
  await interviewStore.compareReports(previousCompletedSessionId.value, sessionId.value)
}

function findTurnReview(turnId: number) {
  return enhancement.value?.turnReviews.find((item) => item.turnId === turnId)
}

onMounted(async () => {
  await interviewStore.fetchReport(sessionId.value)
  await interviewStore.fetchSessions()
  await loadEnhancement()
})

onUnmounted(() => {
  if (enhancementTimer !== undefined) {
    window.clearInterval(enhancementTimer)
  }
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

.interview-report-page__section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.interview-report-page__section-header .interview-report-page__section-title {
  margin: 0;
}

.interview-report-page__tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 12px;
}

.interview-report-page__compare-scores {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
}

.interview-report-page__compare-dim {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--nb-border);
  font-size: 14px;
}
</style>

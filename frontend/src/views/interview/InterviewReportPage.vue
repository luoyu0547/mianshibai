<template>
  <MainLayout>
    <div class="interview-report-page">
      <NbPageHeader
        eyebrow="面试复盘"
        title="面试报告"
        description="AI 面试官的详细评估与改进建议"
      />

      <NbCard v-if="interviewStore.loading">
        <NbLoadingBlock title="加载面试报告..." :rows="5" />
      </NbCard>

      <template v-else-if="report">
        <div class="interview-report-page__score-section">
          <NbStatCard
            label="综合评分"
            :value="report.totalScore"
            hint="满分 100 分"
            variant="primary"
            class="interview-report-page__total-score"
          />
        </div>

        <div class="interview-report-page__dimensions">
          <NbStatCard
            v-for="dim in dimensions"
            :key="dim.key"
            :label="dim.label"
            :value="report[dim.key]"
            :variant="dim.statVariant"
            class="interview-report-page__dim-card"
          >
            <template #action>
              <div class="interview-report-page__dim-bar">
                <div
                  class="interview-report-page__dim-bar-fill"
                  :style="{ width: `${report[dim.key]}%`, background: dim.color }"
                ></div>
              </div>
            </template>
          </NbStatCard>
        </div>

        <NbCard class="interview-report-page__section">
          <NbSectionTitle title="AI 总结" />
          <p class="interview-report-page__summary-text">{{ report.summary }}</p>
        </NbCard>

        <NbCard class="interview-report-page__section">
          <NbSectionTitle title="复盘增强">
            <template #meta>
              <NbStatusBadge
                v-if="enhancement"
                :label="enhancementStatusLabel"
                :variant="enhancementStatusVariant"
              />
            </template>
          </NbSectionTitle>
          <p v-if="!enhancement || enhancement.status === 'pending' || enhancement.status === 'running'" class="interview-report-page__summary-text">
            AI 正在生成逐题复盘和优秀回答，稍后会自动刷新。
          </p>
          <template v-else-if="enhancement.status === 'failed'">
            <p class="interview-report-page__summary-text">{{ enhancement.errorMessage || '复盘增强生成失败' }}</p>
            <NbButton variant="primary" @click="retryEnhancement" style="margin-top: 12px">重新生成</NbButton>
          </template>
          <template v-else>
            <p class="interview-report-page__summary-text">{{ enhancement.summary }}</p>
            <div v-if="reportRadarOption" class="interview-report-page__chart-card">
              <BaseChart :option="reportRadarOption" height="320px" />
            </div>
            <div v-if="enhancement.skillGaps.length > 0" class="interview-report-page__tags">
              <NbStatusBadge
                v-for="gap in enhancement.skillGaps"
                :key="gap.name"
                :label="`${gap.name} · ${gap.severity}`"
                variant="warning"
              />
            </div>
            <div v-if="enhancement.actionItems.length > 0" class="interview-report-page__action-items">
              <div class="interview-report-page__action-header">下一步行动</div>
              <ul class="interview-report-page__suggestions">
                <li v-for="(item, idx) in enhancement.actionItems" :key="idx">{{ item }}</li>
              </ul>
            </div>
          </template>
        </NbCard>

        <NbCard v-if="report.suggestions.length > 0" class="interview-report-page__section">
          <NbSectionTitle title="改进建议" />
          <ul class="interview-report-page__suggestions">
            <li v-for="(suggestion, idx) in report.suggestions" :key="idx">
              {{ suggestion }}
            </li>
          </ul>
        </NbCard>

        <NbCard class="interview-report-page__section">
          <NbSectionTitle title="答题回顾" description="点击展开查看每道题的详细分析" />
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
                    <div class="interview-report-page__turn-tags">
                      <NbStatusBadge
                        v-for="kp in findTurnReview(turn.id)?.knowledgePoints"
                        :key="kp"
                        :label="kp"
                        variant="info"
                      />
                    </div>
                  </div>
                </template>
              </div>
            </el-collapse-item>
          </el-collapse>
        </NbCard>

        <NbCard v-if="previousCompletedSessionId" class="interview-report-page__section">
          <NbSectionTitle title="报告对比" description="与上次面试报告进行趋势对比" />
          <NbButton variant="primary" @click="compareWithPrevious">与上次报告对比</NbButton>
          <div v-if="comparison" class="interview-report-page__comparison">
            <div class="interview-report-page__compare-scores">
              <span>{{ comparison.baseTotalScore }} 分</span>
              <span class="interview-report-page__compare-arrow">&rarr;</span>
              <span :class="comparison.totalDelta >= 0 ? 'interview-report-page__compare-up' : 'interview-report-page__compare-down'">
                {{ comparison.targetTotalScore }} 分（{{ comparison.totalDelta >= 0 ? '+' : '' }}{{ comparison.totalDelta }}）
              </span>
            </div>
            <div class="interview-report-page__compare-dims">
              <div v-for="dim in comparison.dimensions" :key="dim.key" class="interview-report-page__compare-dim">
                <span class="interview-report-page__compare-dim-label">{{ dim.label }}</span>
                <span class="interview-report-page__compare-dim-scores">{{ dim.baseScore }} &rarr; {{ dim.targetScore }}</span>
                <span :class="dim.delta >= 0 ? 'interview-report-page__compare-up' : 'interview-report-page__compare-down'">
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
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import BaseChart from '@/components/charts/BaseChart.vue'
import { buildRadarOption } from '@/utils/charts/reviewCharts'
import { useInterviewStore } from '@/stores/interview'

const route = useRoute()
const interviewStore = useInterviewStore()

const sessionId = computed(() => Number(route.params.id) || 0)
const report = computed(() => interviewStore.currentReport)
const enhancement = computed(() => interviewStore.currentEnhancement)
const comparison = computed(() => interviewStore.currentComparison)

const reportRadarOption = computed(() => {
  if (!enhancement.value?.radar || Object.keys(enhancement.value.radar).length === 0) {
    return null
  }
  return buildRadarOption(enhancement.value.radar, '本次能力雷达')
})

const dimensions = [
  { key: 'accuracyScore' as const, label: '技术准确性', color: '#6C5CE7', statVariant: 'primary' as const },
  { key: 'clarityScore' as const, label: '表达清晰度', color: '#00CEC9', statVariant: 'success' as const },
  { key: 'depthScore' as const, label: '项目深度', color: '#FD79A8', statVariant: 'accent' as const },
  { key: 'matchingScore' as const, label: '岗位匹配度', color: '#00B894', statVariant: 'warning' as const },
]

const enhancementStatusLabel = computed(() => {
  if (!enhancement.value) return ''
  const status = enhancement.value.status
  if (status === 'pending' || status === 'running') return '生成中'
  if (status === 'completed') return '已完成'
  return '失败'
})

const enhancementStatusVariant = computed<'info' | 'success' | 'danger'>(() => {
  if (!enhancement.value) return 'info'
  const status = enhancement.value.status
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  return 'info'
})

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

.interview-report-page__score-section {
  display: flex;
  justify-content: center;
}

.interview-report-page__total-score {
  min-width: 240px;
}

.interview-report-page__total-score :deep(.nb-stat-card__value) {
  font-size: 56px;
}

.interview-report-page__dimensions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.interview-report-page__dim-card {
  padding: 16px 20px;
}

.interview-report-page__dim-bar {
  width: 60px;
  height: 6px;
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius-sm);
  overflow: hidden;
}

.interview-report-page__dim-bar-fill {
  height: 100%;
  border-radius: var(--nb-radius-sm);
  transition: width 0.5s ease;
}

.interview-report-page__section {
  padding: 24px;
}

.interview-report-page__summary-text {
  font-size: 15px;
  line-height: 1.8;
  color: var(--nb-text);
  margin: 0;
}

.interview-report-page__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.interview-report-page__action-items {
  margin-top: 16px;
}

.interview-report-page__action-header {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  color: var(--nb-muted);
  margin-bottom: 8px;
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

.interview-report-page__turn-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.interview-report-page__comparison {
  margin-top: 16px;
}

.interview-report-page__compare-scores {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.interview-report-page__compare-arrow {
  color: var(--nb-muted);
}

.interview-report-page__compare-up {
  color: var(--nb-success);
}

.interview-report-page__compare-down {
  color: var(--nb-danger);
}

.interview-report-page__compare-dims {
  margin-top: 12px;
}

.interview-report-page__compare-dim {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: var(--nb-border);
  font-size: 14px;
}

.interview-report-page__compare-dim-label {
  font-weight: 600;
}

.interview-report-page__compare-dim-scores {
  color: var(--nb-muted);
}

.interview-report-page__chart-card {
  margin-top: 16px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  padding: 12px;
  background: var(--nb-surface);
}
</style>

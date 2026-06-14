<!-- src/views/home/HomePage.vue -->
<template>
  <MainLayout>
    <div class="home-page">
      <NbPageHeader
        eyebrow="指挥中心"
        :title="`欢迎回来，${displayName}`"
        description="AI 驱动的求职作战面板，掌控你的每一步"
      >
        <template #actions>
          <router-link to="/interview/new">
            <NbButton variant="ghost">开始面试</NbButton>
          </router-link>
        </template>
      </NbPageHeader>

      <NbCard v-if="dashboardStore.loading">
        <NbLoadingBlock title="加载作战数据..." :rows="5" />
      </NbCard>

      <NbCard v-else-if="!dashboard">
        <NbEmptyState
          title="暂无作战数据"
          description="无法获取面板数据，请稍后重试"
        >
          <template #action>
            <NbButton variant="primary" @click="dashboardStore.fetchDashboard()">重新加载</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <template v-else>
        <NbCard variant="ai" class="hero-card">
          <div class="hero-card__layout">
            <div class="hero-card__main">
              <h3 class="hero-card__label">今日优先事项</h3>
              <ol v-if="dashboard.todayPriorities.length > 0" class="hero-card__list">
                <li
                  v-for="(item, idx) in dashboard.todayPriorities"
                  :key="idx"
                  class="hero-card__item"
                >
                  <span class="hero-card__num">{{ idx + 1 }}</span>
                  <span class="hero-card__text">{{ item }}</span>
                </li>
              </ol>
              <p v-else class="hero-card__hint">今天还没有安排任务，开始训练或模拟面试吧</p>
            </div>
            <div class="hero-card__cta">
              <router-link to="/training">
                <NbButton variant="primary" block>开始今日训练</NbButton>
              </router-link>
              <router-link to="/interview/new">
                <NbButton variant="ghost" block>模拟面试</NbButton>
              </router-link>
            </div>
          </div>
        </NbCard>

        <section
          v-if="dashboard.applicationStats || dashboard.masterySummary"
          class="home-section"
        >
          <NbSectionTitle title="关键指标" description="投递进度与知识掌握概览" />
          <div class="stat-grid">
            <template v-if="dashboard.applicationStats">
              <NbStatCard
                label="全部投递"
                :value="dashboard.applicationStats.total"
                variant="primary"
              />
              <NbStatCard
                label="面试中"
                :value="dashboard.applicationStats.interviewing"
                variant="warning"
              />
              <NbStatCard
                label="Offer"
                :value="dashboard.applicationStats.offer"
                variant="success"
              />
              <NbStatCard
                label="已结束"
                :value="dashboard.applicationStats.failed"
                variant="danger"
              />
            </template>
            <template v-if="dashboard.masterySummary">
              <NbStatCard
                label="薄弱"
                :value="dashboard.masterySummary.weak"
                variant="danger"
              />
              <NbStatCard
                label="基础"
                :value="dashboard.masterySummary.basic"
                variant="warning"
              />
              <NbStatCard
                label="良好"
                :value="dashboard.masterySummary.good"
                variant="primary"
              />
              <NbStatCard
                label="掌握"
                :value="dashboard.masterySummary.mastered"
                variant="success"
              />
            </template>
          </div>
        </section>

        <section class="home-section">
          <NbSectionTitle title="训练计划" description="当前进行中的八股训练">
            <template #actions>
              <router-link v-if="dashboard.activePlan" to="/training">
                <NbButton variant="ghost">查看全部</NbButton>
              </router-link>
            </template>
          </NbSectionTitle>

          <NbCard v-if="dashboard.activePlan">
            <div class="plan-info">
              <h4 class="plan-info__title">{{ dashboard.activePlan.title }}</h4>
              <el-progress
                :percentage="planProgress"
                :stroke-width="12"
                :format="planProgressFormat"
                class="plan-info__progress"
              />
              <div
                v-if="dashboard.activePlan.focusTopics.length > 0"
                class="plan-info__topics"
              >
                <el-tag
                  v-for="topic in dashboard.activePlan.focusTopics"
                  :key="topic"
                  size="small"
                  class="nb-tag"
                >
                  {{ topic }}
                </el-tag>
              </div>
              <router-link to="/training">
                <NbButton variant="primary" block>继续训练</NbButton>
              </router-link>
            </div>
          </NbCard>

          <NbCard v-else>
            <NbEmptyState
              title="暂无进行中的训练计划"
              description="AI 将根据你的简历和目标岗位，智能生成八股训练计划"
            >
              <template #action>
                <router-link to="/training">
                  <NbButton variant="primary">生成训练计划</NbButton>
                </router-link>
              </template>
            </NbEmptyState>
          </NbCard>

          <NbCard v-if="dashboard.pendingQuestions.length > 0">
            <h3 class="card-subtitle">待复习题目</h3>
            <div class="question-list">
              <div
                v-for="q in dashboard.pendingQuestions"
                :key="q.id"
                class="question-item"
                @click="router.push(`/training/question/${q.id}`)"
              >
                <span class="question-item__title">{{ q.title }}</span>
                <NbStatusBadge
                  :label="QUESTION_STATUS_LABELS[q.status]"
                  :variant="questionStatusVariant(q.status)"
                />
              </div>
            </div>
          </NbCard>

          <NbCard v-if="dashboard.weakTopics.length > 0" variant="warning">
            <h3 class="card-subtitle">薄弱知识点</h3>
            <div class="tag-cloud">
              <el-tag
                v-for="topic in dashboard.weakTopics"
                :key="topic"
                type="danger"
                size="small"
                class="nb-tag"
              >
                {{ topic }}
              </el-tag>
            </div>
          </NbCard>
        </section>

        <section
          v-if="dashboard.algorithmRecommendations.length > 0"
          class="home-section"
        >
          <NbSectionTitle title="算法推荐" description="AI 为你推荐的算法练习" />
          <NbCard variant="accent">
            <div class="algo-list">
              <div
                v-for="algo in dashboard.algorithmRecommendations"
                :key="algo.id"
                class="algo-item"
              >
                <div class="algo-item__info">
                  <span class="algo-item__platform">[{{ algo.platform }}]</span>
                  <span class="algo-item__ref">{{ algo.problemRef }}</span>
                  <span class="algo-item__reason">{{ algo.reason }}</span>
                </div>
                <NbButton
                  v-if="!algo.completed"
                  variant="success"
                  @click="handleCompleteAlgo(algo.id)"
                >
                  完成
                </NbButton>
                <NbStatusBadge v-else label="已完成" variant="success" />
              </div>
            </div>
          </NbCard>
        </section>

        <section
          v-if="dashboard.reviewQuestions.length > 0 || dashboard.weakMasteries.length > 0"
          class="home-section"
        >
          <NbSectionTitle title="复习与掌握" description="今日复习题与薄弱掌握点" />

          <NbCard v-if="dashboard.reviewQuestions.length > 0">
            <h3 class="card-subtitle">今日复习</h3>
            <div class="review-list">
              <div
                v-for="item in dashboard.reviewQuestions.slice(0, 5)"
                :key="item.questionId"
                class="review-item"
                @click="router.push(`/training/question/${item.questionId}`)"
              >
                <span class="review-item__title">{{ item.title }}</span>
                <NbStatusBadge
                  :label="masteryDescriptor(item.masteryLevel).label"
                  :variant="masteryDescriptor(item.masteryLevel).variant"
                />
                <span class="review-item__score">{{ item.latestScore }} 分</span>
              </div>
            </div>
          </NbCard>

          <NbCard v-if="dashboard.weakMasteries.length > 0" variant="warning">
            <h3 class="card-subtitle">薄弱掌握</h3>
            <div class="tag-row">
              <el-tag
                v-for="item in dashboard.weakMasteries.slice(0, 5)"
                :key="item.id"
                :color="masteryColor(item.masteryLevel)"
                size="small"
                effect="dark"
                style="border: none; color: #fff; cursor: pointer;"
                @click="router.push({ path: '/training/mistakes', query: { topic: item.targetName } })"
              >
                {{ item.targetName }}
              </el-tag>
            </div>
          </NbCard>
        </section>

        <section class="home-section">
          <NbSectionTitle title="快速操作" description="一键进入核心功能" />
          <div class="action-grid">
            <NbCard hoverable class="action-card">
              <div class="action-card__icon" style="background: var(--nb-primary);">
                <IconTarget />
              </div>
              <h3 class="action-card__title">开始面试</h3>
              <p class="action-card__desc">选择岗位方向，开始 AI 模拟面试</p>
              <router-link to="/interview/new">
                <NbButton variant="primary" block>立即开始</NbButton>
              </router-link>
            </NbCard>

            <NbCard hoverable class="action-card">
              <div class="action-card__icon" style="background: var(--nb-secondary);">
                <IconChart />
              </div>
              <h3 class="action-card__title">生成训练计划</h3>
              <p class="action-card__desc">AI 智能生成八股训练计划</p>
              <router-link to="/training">
                <NbButton variant="secondary" block>前往训练</NbButton>
              </router-link>
            </NbCard>

            <NbCard hoverable class="action-card">
              <div class="action-card__icon" style="background: var(--nb-accent);">
                <IconResume />
              </div>
              <h3 class="action-card__title">管理投递</h3>
              <p class="action-card__desc">追踪投递进度与待办事项</p>
              <router-link to="/applications">
                <NbButton variant="accent" block>查看投递</NbButton>
              </router-link>
            </NbCard>
          </div>
        </section>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import IconTarget from '@/components/icons/IconTarget.vue'
import IconChart from '@/components/icons/IconChart.vue'
import IconResume from '@/components/icons/IconResume.vue'
import { useUserStore } from '@/stores/user'
import { useDashboardStore } from '@/stores/dashboard'
import { useTrainingStore } from '@/stores/training'
import { QUESTION_STATUS_LABELS, MASTERY_LEVEL_OPTIONS } from '@/types/training'
import type { TrainingQuestionStatus, MasteryLevel } from '@/types/training'
import { getStatusDescriptor, trainingMasteryMap } from '@/utils/statusMaps'
import type { StatusVariant } from '@/utils/statusMaps'

const router = useRouter()
const userStore = useUserStore()
const dashboardStore = useDashboardStore()
const trainingStore = useTrainingStore()

const dashboard = computed(() => dashboardStore.dashboard)

const displayName = computed(
  () => userStore.userInfo?.userName || userStore.userInfo?.userAccount || '求职者',
)

const planProgress = computed(() => {
  const plan = dashboard.value?.activePlan
  if (!plan || plan.questions.length === 0) return 0
  const reviewed = plan.questions.filter(
    (q) => q.status === 'reviewed' || q.status === 'mastered',
  ).length
  return Math.round((reviewed / plan.questions.length) * 100)
})

function planProgressFormat() {
  const plan = dashboard.value?.activePlan
  if (!plan) return ''
  const reviewed = plan.questions.filter(
    (q) => q.status === 'reviewed' || q.status === 'mastered',
  ).length
  return `${reviewed}/${plan.questions.length}`
}

const questionStatusVariantMap: Record<TrainingQuestionStatus, StatusVariant> = {
  mastered: 'success',
  reviewed: 'default',
  answered: 'warning',
  skipped: 'info',
  pending: 'danger',
}

function questionStatusVariant(status: TrainingQuestionStatus): StatusVariant {
  return questionStatusVariantMap[status] ?? 'default'
}

async function handleCompleteAlgo(id: number) {
  const ok = await trainingStore.completeAlgorithm(id)
  if (ok) {
    ElMessage.success('已标记完成')
    dashboardStore.fetchDashboard()
  }
}

function masteryColor(level: MasteryLevel) {
  return MASTERY_LEVEL_OPTIONS.find((o) => o.value === level)?.color || '#999'
}

function masteryDescriptor(level: MasteryLevel) {
  return getStatusDescriptor(trainingMasteryMap, level)
}

onMounted(() => {
  dashboardStore.fetchDashboard()
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.home-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card__layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.hero-card__main {
  flex: 1;
  min-width: 0;
}

.hero-card__label {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--nb-ink);
}

.hero-card__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-card__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-bg);
}

.hero-card__num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--nb-primary);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.hero-card__text {
  font-size: 15px;
}

.hero-card__hint {
  font-size: 14px;
  color: var(--nb-muted);
  margin: 0;
}

.hero-card__cta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 200px;
  flex-shrink: 0;
}

.hero-card__cta a {
  text-decoration: none;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.card-subtitle {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 4px;
}

.plan-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.plan-info__title {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.plan-info__progress {
  width: 100%;
}

.plan-info__topics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nb-tag {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-bg);
  cursor: pointer;
  transition: var(--nb-transition);
}

.question-item:hover {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-1px, -1px);
}

.question-item__title {
  font-weight: 500;
  font-size: 14px;
  flex: 1;
  margin-right: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.algo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.algo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-bg);
}

.algo-item__info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  font-size: 14px;
  flex-wrap: wrap;
}

.algo-item__platform {
  font-weight: 600;
  color: var(--nb-primary);
}

.algo-item__ref {
  font-weight: 500;
}

.algo-item__reason {
  color: var(--nb-muted);
  font-size: 13px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.review-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-bg);
  cursor: pointer;
  transition: var(--nb-transition);
}

.review-item:hover {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-1px, -1px);
}

.review-item__title {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-item__score {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
  color: var(--nb-primary);
  white-space: nowrap;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.action-card {
  text-align: center;
}

.action-card__icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #fff;
}

.action-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
}

.action-card__desc {
  font-size: 14px;
  color: var(--nb-muted);
  margin: 0 0 20px;
}

.action-grid a {
  text-decoration: none;
}

@media (max-width: 768px) {
  .hero-card__layout {
    flex-direction: column;
  }

  .hero-card__cta {
    width: 100%;
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

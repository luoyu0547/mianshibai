<!-- src/views/home/HomePage.vue -->
<template>
  <MainLayout>
    <div class="home-page">
      <NbCard class="welcome-card">
        <div class="welcome-card__content">
          <div class="welcome-card__greeting">
            <span class="welcome-card__emoji">👋</span>
            <h2 class="welcome-card__title">
              欢迎回来，{{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || '求职者' }}
            </h2>
          </div>
          <p class="welcome-card__subtitle">今日作战计划</p>
        </div>
      </NbCard>

      <div v-if="dashboardStore.loading" class="home-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="dashboard">
        <NbCard v-if="dashboard.todayPriorities.length > 0" class="section-card">
          <h3 class="section-title">今日优先事项</h3>
          <ul class="priority-list">
            <li v-for="(item, idx) in dashboard.todayPriorities" :key="idx" class="priority-item">
              <span class="priority-item__index">{{ idx + 1 }}</span>
              <span class="priority-item__text">{{ item }}</span>
            </li>
          </ul>
        </NbCard>

        <div v-if="dashboard.applicationStats" class="home-page__stats">
          <NbCard hoverable class="stat-card">
            <div class="stat-card__value">{{ dashboard.applicationStats.total }}</div>
            <div class="stat-card__label">全部投递</div>
          </NbCard>
          <NbCard hoverable class="stat-card">
            <div class="stat-card__value stat-card__value--warning">{{ dashboard.applicationStats.interviewing }}</div>
            <div class="stat-card__label">面试中</div>
          </NbCard>
          <NbCard hoverable class="stat-card">
            <div class="stat-card__value stat-card__value--success">{{ dashboard.applicationStats.offer }}</div>
            <div class="stat-card__label">Offer</div>
          </NbCard>
          <NbCard hoverable class="stat-card">
            <div class="stat-card__value stat-card__value--danger">{{ dashboard.applicationStats.failed }}</div>
            <div class="stat-card__label">已结束</div>
          </NbCard>
        </div>

        <NbCard class="section-card">
          <h3 class="section-title">当前训练计划</h3>
          <template v-if="dashboard.activePlan">
            <div class="plan-info">
              <h4 class="plan-info__title">{{ dashboard.activePlan.title }}</h4>
              <el-progress
                :percentage="planProgress"
                :stroke-width="12"
                :format="planProgressFormat"
                class="plan-info__progress"
              />
              <div v-if="dashboard.activePlan.focusTopics.length > 0" class="plan-info__topics">
                <el-tag
                  v-for="topic in dashboard.activePlan.focusTopics"
                  :key="topic"
                  size="small"
                  class="nb-tag"
                >
                  {{ topic }}
                </el-tag>
              </div>
            </div>
          </template>
          <template v-else>
            <p class="empty-hint">暂无进行中的训练计划</p>
            <router-link to="/training">
              <NbButton type="primary">生成训练计划</NbButton>
            </router-link>
          </template>
        </NbCard>

        <NbCard v-if="dashboard.pendingQuestions.length > 0" class="section-card">
          <h3 class="section-title">待复习题目</h3>
          <div class="question-list">
            <div
              v-for="q in dashboard.pendingQuestions"
              :key="q.id"
              class="question-item"
              @click="router.push(`/training/question/${q.id}`)"
            >
              <span class="question-item__title">{{ q.title }}</span>
              <el-tag
                size="small"
                :type="questionStatusTagType(q.status)"
                class="nb-tag"
              >
                {{ QUESTION_STATUS_LABELS[q.status] }}
              </el-tag>
            </div>
          </div>
        </NbCard>

        <NbCard v-if="dashboard.weakTopics.length > 0" class="section-card">
          <h3 class="section-title">薄弱知识点</h3>
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

        <NbCard v-if="dashboard.algorithmRecommendations.length > 0" class="section-card">
          <h3 class="section-title">算法推荐</h3>
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
                type="success"
                @click="handleCompleteAlgo(algo.id)"
              >
                完成
              </NbButton>
              <el-tag v-else type="success" size="small" class="nb-tag">已完成</el-tag>
            </div>
          </div>
        </NbCard>
      </template>

      <div class="home-page__actions">
        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-primary);">
            <IconTarget />
          </div>
          <h3 class="action-card__title">开始面试</h3>
          <p class="action-card__desc">选择岗位方向，开始 AI 模拟面试</p>
          <router-link to="/interview/new">
            <NbButton type="primary" block>立即开始</NbButton>
          </router-link>
        </NbCard>

        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-secondary);">
            <IconChart />
          </div>
          <h3 class="action-card__title">生成训练计划</h3>
          <p class="action-card__desc">AI 智能生成八股训练计划</p>
          <router-link to="/training">
            <NbButton type="secondary" block>前往训练</NbButton>
          </router-link>
        </NbCard>

        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-accent);">
            <IconResume />
          </div>
          <h3 class="action-card__title">管理投递</h3>
          <p class="action-card__desc">追踪投递进度与待办事项</p>
          <router-link to="/applications">
            <NbButton type="accent" block>查看投递</NbButton>
          </router-link>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import IconTarget from '@/components/icons/IconTarget.vue'
import IconChart from '@/components/icons/IconChart.vue'
import IconResume from '@/components/icons/IconResume.vue'
import { useUserStore } from '@/stores/user'
import { useDashboardStore } from '@/stores/dashboard'
import { useTrainingStore } from '@/stores/training'
import { QUESTION_STATUS_LABELS } from '@/types/training'
import type { TrainingQuestionStatus } from '@/types/training'

const router = useRouter()
const userStore = useUserStore()
const dashboardStore = useDashboardStore()
const trainingStore = useTrainingStore()

const dashboard = computed(() => dashboardStore.dashboard)

const planProgress = computed(() => {
  const plan = dashboard.value?.activePlan
  if (!plan || plan.questions.length === 0) return 0
  const reviewed = plan.questions.filter(
    (q) => q.status === 'reviewed' || q.status === 'mastered',
  ).length
  return Math.round((reviewed / plan.questions.length) * 100)
})

function planProgressFormat(percentage: number) {
  const plan = dashboard.value?.activePlan
  if (!plan) return ''
  const reviewed = plan.questions.filter(
    (q) => q.status === 'reviewed' || q.status === 'mastered',
  ).length
  return `${reviewed}/${plan.questions.length}`
}

function questionStatusTagType(status: TrainingQuestionStatus) {
  if (status === 'mastered') return 'success'
  if (status === 'reviewed') return ''
  if (status === 'answered') return 'warning'
  if (status === 'skipped') return 'info'
  return 'danger'
}

async function handleCompleteAlgo(id: number) {
  const ok = await trainingStore.completeAlgorithm(id)
  if (ok) {
    ElMessage.success('已标记完成')
    dashboardStore.fetchDashboard()
  }
}

onMounted(() => {
  dashboardStore.fetchDashboard()
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-card__content {
  text-align: center;
}

.welcome-card__greeting {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.welcome-card__emoji {
  font-size: 32px;
}

.welcome-card__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.welcome-card__subtitle {
  font-size: 16px;
  color: var(--nb-muted);
  margin: 0;
}

.home-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.section-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.priority-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.priority-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-bg);
}

.priority-item__index {
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

.priority-item__text {
  font-size: 15px;
}

.home-page__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  text-align: center;
  padding: 24px 16px;
}

.stat-card__value {
  font-family: var(--font-heading);
  font-size: 36px;
  font-weight: 700;
  color: var(--nb-primary);
  margin-bottom: 4px;
}

.stat-card__value--warning {
  color: var(--nb-warning);
}

.stat-card__value--success {
  color: var(--nb-success);
}

.stat-card__value--danger {
  color: var(--nb-accent);
}

.stat-card__label {
  font-size: 13px;
  color: var(--nb-muted);
}

.plan-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.empty-hint {
  color: var(--nb-muted);
  margin: 0 0 12px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.home-page__actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
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
  margin-bottom: 20px;
}

.home-page__actions a {
  text-decoration: none;
}

@media (max-width: 768px) {
  .home-page__stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .home-page__actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .home-page__actions {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

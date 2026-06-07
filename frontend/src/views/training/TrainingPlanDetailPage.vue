<!-- src/views/training/TrainingPlanDetailPage.vue -->
<template>
  <MainLayout>
    <div class="plan-detail">
      <div v-if="trainingStore.loading" class="plan-detail__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="plan">
        <div class="plan-detail__header">
          <el-button text @click="router.back()">&larr; 返回</el-button>
        </div>

        <div class="plan-detail__hero">
          <div class="plan-detail__hero-left">
            <h1 class="plan-detail__title">{{ plan.title }}</h1>
            <p v-if="plan.summary" class="plan-detail__summary">{{ plan.summary }}</p>
            <div class="plan-detail__meta">
              <el-tag
                :type="planStatusTagType(plan.status)"
                size="small"
                class="nb-tag"
              >
                {{ planStatusLabel(plan.status) }}
              </el-tag>
              <span class="plan-detail__meta-item">{{ plan.targetDays }} 天计划</span>
            </div>
            <div v-if="plan.focusTopics.length > 0" class="tag-cloud" style="margin-top: 12px;">
              <el-tag
                v-for="topic in plan.focusTopics"
                :key="topic"
                size="small"
                class="nb-tag"
              >
                {{ topic }}
              </el-tag>
            </div>
          </div>
        </div>

        <NbCard
          v-for="group in groupedQuestions"
          :key="group.dayIndex"
          class="section-card"
        >
          <h3 class="section-title">Day {{ group.dayIndex }}</h3>
          <div class="question-list">
            <div
              v-for="q in group.questions"
              :key="q.id"
              class="question-item"
              @click="router.push(`/training/question/${q.id}`)"
            >
              <div class="question-item__info">
                <span class="question-item__title">{{ q.title }}</span>
                <el-tag size="small" class="nb-tag">{{ q.topic }}</el-tag>
                <el-tag
                  :type="difficultyTagType(q.difficulty)"
                  size="small"
                  class="nb-tag"
                >
                  {{ difficultyLabel(q.difficulty) }}
                </el-tag>
                <el-tag
                  :type="questionStatusTagType(q.status)"
                  size="small"
                  class="nb-tag"
                >
                  {{ QUESTION_STATUS_LABELS[q.status] }}
                </el-tag>
              </div>
              <div class="question-item__score">
                <span v-if="q.latestScore !== null" class="question-item__score-val">
                  {{ q.latestScore }}分
                </span>
                <el-tag
                  v-if="q.latestMasteryLevel"
                  :color="masteryColor(q.latestMasteryLevel)"
                  size="small"
                  class="nb-tag"
                  style="color: #fff; border-color: transparent;"
                >
                  {{ masteryLabel(q.latestMasteryLevel) }}
                </el-tag>
              </div>
            </div>
          </div>
        </NbCard>

        <NbCard
          v-if="plan.algorithmRecommendations.length > 0"
          class="section-card"
        >
          <h3 class="section-title">算法推荐</h3>
          <div class="algo-list">
            <div
              v-for="algo in plan.algorithmRecommendations"
              :key="algo.id"
              class="algo-item"
            >
              <div class="algo-item__info">
                <el-tag size="small" class="nb-tag">{{ algo.category }}</el-tag>
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
              <NbButton
                v-else
                @click="handleReopenAlgo(algo.id)"
              >
                重开
              </NbButton>
            </div>
          </div>
        </NbCard>

        <div v-if="plan.status === 'active'" class="plan-detail__footer">
          <NbButton @click="handleArchive">归档计划</NbButton>
          <NbButton type="success" @click="handleCompletePlan">标记完成</NbButton>
        </div>
      </template>

      <div v-else class="plan-detail__empty">
        <p>未找到该训练计划</p>
        <el-button type="primary" text @click="router.push('/training')">返回训练中心</el-button>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useTrainingStore } from '@/stores/training'
import {
  QUESTION_STATUS_LABELS,
  TRAINING_DIFFICULTY_OPTIONS,
  MASTERY_LEVEL_OPTIONS,
} from '@/types/training'
import type {
  TrainingQuestionStatus,
  TrainingDifficulty,
  TrainingPlanStatus,
  MasteryLevel,
  TrainingQuestionVO,
} from '@/types/training'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const plan = computed(() => trainingStore.currentPlan)

const groupedQuestions = computed(() => {
  if (!plan.value) return []
  const map = new Map<number, TrainingQuestionVO[]>()
  for (const q of plan.value.questions) {
    const list = map.get(q.dayIndex) || []
    list.push(q)
    map.set(q.dayIndex, list)
  }
  return Array.from(map.entries())
    .sort(([a], [b]) => a - b)
    .map(([dayIndex, questions]) => ({ dayIndex, questions }))
})

function planStatusTagType(status: TrainingPlanStatus) {
  if (status === 'completed') return 'success'
  if (status === 'archived') return 'info'
  return ''
}

function planStatusLabel(status: TrainingPlanStatus) {
  if (status === 'active') return '进行中'
  if (status === 'completed') return '已完成'
  return '已归档'
}

function questionStatusTagType(status: TrainingQuestionStatus) {
  if (status === 'mastered') return 'success'
  if (status === 'reviewed') return ''
  if (status === 'answered') return 'warning'
  if (status === 'skipped') return 'info'
  return 'danger'
}

function difficultyTagType(difficulty: TrainingDifficulty) {
  if (difficulty === 'hard') return 'danger'
  if (difficulty === 'medium') return 'warning'
  return 'success'
}

function difficultyLabel(difficulty: TrainingDifficulty) {
  return TRAINING_DIFFICULTY_OPTIONS.find((o) => o.value === difficulty)?.label || difficulty
}

function masteryColor(level: MasteryLevel) {
  return MASTERY_LEVEL_OPTIONS.find((o) => o.value === level)?.color || '#999'
}

function masteryLabel(level: MasteryLevel) {
  return MASTERY_LEVEL_OPTIONS.find((o) => o.value === level)?.label || level
}

async function handleCompleteAlgo(id: number) {
  const ok = await trainingStore.completeAlgorithm(id)
  if (ok) {
    ElMessage.success('已标记完成')
    trainingStore.fetchPlan(Number(route.params.id))
  }
}

async function handleReopenAlgo(id: number) {
  const ok = await trainingStore.reopenAlgorithm(id)
  if (ok) {
    ElMessage.success('已重新打开')
    trainingStore.fetchPlan(Number(route.params.id))
  }
}

async function handleArchive() {
  const id = Number(route.params.id)
  const ok = await trainingStore.archivePlan(id)
  if (ok) {
    ElMessage.success('计划已归档')
    router.push('/training')
  } else {
    ElMessage.error('操作失败')
  }
}

async function handleCompletePlan() {
  const id = Number(route.params.id)
  const ok = await trainingStore.completePlan(id)
  if (ok) {
    ElMessage.success('计划已标记完成')
    router.push('/training')
  } else {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    trainingStore.fetchPlan(id)
  }
})
</script>

<style scoped>
.plan-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.plan-detail__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.plan-detail__header {
  display: flex;
  align-items: center;
}

.plan-detail__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.plan-detail__hero-left {
  flex: 1;
}

.plan-detail__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.plan-detail__summary {
  color: var(--nb-muted);
  font-size: 15px;
  margin: 0 0 12px;
  line-height: 1.5;
}

.plan-detail__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plan-detail__meta-item {
  font-size: 14px;
  color: var(--nb-muted);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nb-tag {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
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

.question-item__info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  margin-right: 12px;
  overflow: hidden;
}

.question-item__title {
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-item__score {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.question-item__score-val {
  font-weight: 600;
  font-size: 14px;
  color: var(--nb-primary);
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

.plan-detail__footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.plan-detail__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}
</style>

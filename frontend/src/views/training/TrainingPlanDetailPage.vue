<template>
  <MainLayout>
    <div class="plan-detail">
      <NbCard v-if="trainingStore.loading">
        <NbLoadingBlock title="加载计划详情..." :rows="4" />
      </NbCard>

      <template v-else-if="plan">
        <div class="plan-detail__back">
          <NbButton variant="ghost" @click="router.back()">&larr; 返回</NbButton>
        </div>

        <NbPageHeader
          :title="plan.title"
          :description="plan.summary || undefined"
        >
          <template #actions>
            <NbStatusBadge
              :label="planStatusLabel(plan.status)"
              :variant="plan.status === 'completed' ? 'success' : plan.status === 'archived' ? 'muted' : 'primary'"
            />
          </template>
        </NbPageHeader>

        <div class="plan-detail__meta">
          <NbStatusBadge :label="`${plan.targetDays} 天计划`" variant="info" />
          <NbStatusBadge
            v-for="topic in plan.focusTopics"
            :key="topic"
            :label="topic"
            variant="default"
          />
        </div>

        <NbCard
          v-for="group in groupedQuestions"
          :key="group.dayIndex"
        >
          <NbSectionTitle :title="`Day ${group.dayIndex}`" />
          <div class="question-list">
            <div
              v-for="q in group.questions"
              :key="q.id"
              class="question-item"
              @click="router.push(`/training/question/${q.id}`)"
            >
              <div class="question-item__info">
                <span class="question-item__title">{{ q.title }}</span>
                <NbStatusBadge :label="q.topic" variant="info" />
                <NbStatusBadge
                  :label="getStatusDescriptor(trainingDifficultyMap, q.difficulty).label"
                  :variant="getStatusDescriptor(trainingDifficultyMap, q.difficulty).variant"
                />
                <NbStatusBadge
                  :label="getStatusDescriptor(trainingQuestionStatusMap, q.status).label"
                  :variant="getStatusDescriptor(trainingQuestionStatusMap, q.status).variant"
                />
              </div>
              <div class="question-item__score">
                <span v-if="q.latestScore !== null" class="question-item__score-val">
                  {{ q.latestScore }}分
                </span>
                <NbStatusBadge
                  v-if="q.latestMasteryLevel"
                  :label="getStatusDescriptor(trainingMasteryMap, q.latestMasteryLevel).label"
                  :variant="getStatusDescriptor(trainingMasteryMap, q.latestMasteryLevel).variant"
                />
              </div>
            </div>
          </div>
        </NbCard>

        <NbCard v-if="plan.algorithmRecommendations.length > 0">
          <NbSectionTitle title="算法推荐" />
          <div class="algo-list">
            <NbCard
              v-for="algo in plan.algorithmRecommendations"
              :key="algo.id"
              compact
              :variant="algo.completed ? 'success' : 'warning'"
            >
              <div class="algo-item">
                <div class="algo-item__info">
                  <NbStatusBadge :label="algo.category" variant="info" />
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
                <NbButton
                  v-else
                  @click="handleReopenAlgo(algo.id)"
                >
                  重开
                </NbButton>
              </div>
            </NbCard>
          </div>
        </NbCard>

        <div v-if="plan.status === 'active'" class="plan-detail__footer">
          <NbButton @click="handleArchive">归档计划</NbButton>
          <NbButton variant="success" @click="handleCompletePlan">标记完成</NbButton>
        </div>
      </template>

      <NbCard v-else>
        <NbEmptyState title="未找到该训练计划">
          <template #action>
            <NbButton variant="primary" @click="router.push('/training')">返回训练中心</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useTrainingStore } from '@/stores/training'
import type { TrainingQuestionVO, TrainingPlanStatus } from '@/types/training'
import {
  trainingMasteryMap,
  trainingQuestionStatusMap,
  trainingDifficultyMap,
  getStatusDescriptor,
} from '@/utils/statusMaps'

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

function planStatusLabel(status: TrainingPlanStatus) {
  if (status === 'active') return '进行中'
  if (status === 'completed') return '已完成'
  return '已归档'
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
  gap: 24px;
}

.plan-detail__back {
  display: flex;
  align-items: center;
}

.plan-detail__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
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
  margin-top: 16px;
}

.algo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
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
</style>

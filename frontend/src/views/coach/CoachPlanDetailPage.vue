<template>
  <MainLayout>
    <div class="plan-page">
      <NbPageHeader
        eyebrow="Coach"
        title="行动计划详情"
      >
        <template #actions>
          <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="coachStore.loading">
        <NbLoadingBlock title="加载计划..." :rows="5" />
      </NbCard>

      <template v-else-if="coachStore.currentPlan">
        <NbCard variant="accent">
          <div class="plan-header">
            <div class="plan-header__main">
              <h2 class="plan-header__title">{{ coachStore.currentPlan.title }}</h2>
              <p class="plan-header__summary">{{ coachStore.currentPlan.summary }}</p>
            </div>
            <div class="plan-header__stat">
              <span class="plan-header__stat-value">{{ planProgress }}%</span>
              <span class="plan-header__stat-label">
                {{ coachStore.currentPlan.completedTaskCount }}/{{ coachStore.currentPlan.totalTaskCount }} 已完成
              </span>
            </div>
          </div>
          <el-progress :percentage="planProgress" :stroke-width="14" />
        </NbCard>

        <div v-for="group in groupedTasks" :key="group.dayIndex" class="day-section">
          <NbSectionTitle :title="`Day ${group.dayIndex}`">
            <template #meta>
              {{ group.tasks.filter((t) => t.status === 'completed').length }}/{{ group.tasks.length }} 已完成
            </template>
          </NbSectionTitle>
          <NbCard
            v-for="task in group.tasks"
            :key="task.id"
            compact
            :variant="task.status === 'completed' ? 'success' : 'default'"
            class="task-card"
          >
            <div class="task-card__main">
              <div class="task-card__info">
                <div class="task-card__title-row">
                  <h3 class="task-card__title">{{ task.title }}</h3>
                  <NbStatusBadge :label="COACH_TASK_TYPE_LABELS[task.taskType]" variant="info" />
                </div>
                <p class="task-card__desc">{{ task.description }}</p>
                <div class="task-card__badges">
                  <NbStatusBadge
                    :label="`${COACH_PRIORITY_LABELS[task.priority]}优先级`"
                    :variant="todoPriorityMap[task.priority]?.variant ?? 'default'"
                  />
                  <NbStatusBadge
                    :label="getStatusDescriptor(coachTaskStatusMap, task.status).label"
                    :variant="getStatusDescriptor(coachTaskStatusMap, task.status).variant"
                  />
                </div>
              </div>
              <div class="task-card__actions">
                <NbButton
                  v-if="task.status === 'pending'"
                  variant="success"
                  @click="handleComplete(task.id)"
                >
                  完成
                </NbButton>
                <NbButton
                  v-else
                  @click="handleReopen(task.id)"
                >
                  重开
                </NbButton>
                <NbButton
                  v-if="referencePath(task)"
                  @click="router.push(referencePath(task)!)"
                >
                  去处理
                </NbButton>
              </div>
            </div>
          </NbCard>
        </div>
      </template>

      <NbCard v-else>
        <NbEmptyState title="计划不存在" description="该计划可能已被删除。">
          <template #action>
            <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
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
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useCoachStore } from '@/stores/coach'
import { COACH_PRIORITY_LABELS, COACH_TASK_TYPE_LABELS } from '@/types/coach'
import type { CoachTaskVO } from '@/types/coach'
import { coachTaskStatusMap, getStatusDescriptor, todoPriorityMap } from '@/utils/statusMaps'

const route = useRoute()
const router = useRouter()
const coachStore = useCoachStore()

const planProgress = computed(() => {
  const plan = coachStore.currentPlan
  if (!plan || plan.totalTaskCount === 0) return 0
  return Math.round((plan.completedTaskCount / plan.totalTaskCount) * 100)
})

const groupedTasks = computed(() => {
  const map = new Map<number, CoachTaskVO[]>()
  for (const task of coachStore.currentPlan?.tasks || []) {
    const list = map.get(task.dayIndex) || []
    list.push(task)
    map.set(task.dayIndex, list)
  }
  return Array.from(map.entries()).sort(([a], [b]) => a - b).map(([dayIndex, tasks]) => ({ dayIndex, tasks }))
})

function referencePath(task: CoachTaskVO) {
  if (!task.referenceType || !task.referenceId) return null
  if (task.referenceType === 'resume') return `/resume/${task.referenceId}/edit`
  if (task.referenceType === 'interview_session') return `/interview/${task.referenceId}/report`
  if (task.referenceType === 'training_question') return `/training/question/${task.referenceId}`
  if (task.referenceType === 'training_plan') return `/training/plan/${task.referenceId}`
  if (task.referenceType === 'job_application') return `/applications/${task.referenceId}`
  if (task.referenceType === 'job') return `/job/${task.referenceId}`
  return null
}

async function handleComplete(id: number) {
  if (await coachStore.completeTask(id)) {
    ElMessage.success('任务已完成')
  } else {
    ElMessage.error('操作失败，请重试')
  }
}

async function handleReopen(id: number) {
  if (await coachStore.reopenTask(id)) {
    ElMessage.success('任务已重开')
  } else {
    ElMessage.error('操作失败，请重试')
  }
}

onMounted(() => coachStore.fetchPlan(Number(route.params.id)))
</script>

<style scoped>
.plan-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.plan-header__main {
  min-width: 0;
}

.plan-header__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
}

.plan-header__summary {
  margin: 6px 0 0;
  color: var(--nb-muted);
  font-size: 14px;
  line-height: 1.5;
}

.plan-header__stat {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 20px;
  border: var(--nb-border);
  border-radius: var(--nb-radius-lg);
  background: var(--nb-surface);
  box-shadow: var(--nb-shadow-sm);
}

.plan-header__stat-value {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
  color: var(--nb-primary);
}

.plan-header__stat-label {
  font-size: 12px;
  color: var(--nb-muted);
}

.day-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.task-card__info {
  min-width: 0;
  flex: 1;
}

.task-card__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.task-card__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
}

.task-card__desc {
  margin: 6px 0 0;
  color: var(--nb-muted);
  font-size: 14px;
  line-height: 1.5;
}

.task-card__badges {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.task-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .task-card__main {
    flex-direction: column;
  }

  .task-card__actions {
    justify-content: flex-start;
  }

  .plan-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

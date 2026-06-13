<template>
  <MainLayout>
    <div class="plan-page">
      <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
      <NbCard v-if="coachStore.currentPlan" class="plan-header">
        <h1>{{ coachStore.currentPlan.title }}</h1>
        <p>{{ coachStore.currentPlan.summary }}</p>
        <el-progress :percentage="planProgress" :stroke-width="12" />
        <p>{{ coachStore.currentPlan.completedTaskCount }}/{{ coachStore.currentPlan.totalTaskCount }} 个任务已完成</p>
      </NbCard>

      <div v-for="group in groupedTasks" :key="group.dayIndex" class="day-section">
        <h2>Day {{ group.dayIndex }}</h2>
        <NbCard v-for="task in group.tasks" :key="task.id" class="task-card">
          <div class="task-card__main">
            <div>
              <h3>{{ task.title }}</h3>
              <p>{{ task.description }}</p>
              <div class="tags">
                <el-tag>{{ COACH_TASK_TYPE_LABELS[task.taskType] }}</el-tag>
                <el-tag :type="task.priority === 'high' ? 'danger' : task.priority === 'medium' ? 'warning' : 'info'">{{ COACH_PRIORITY_LABELS[task.priority] }}优先级</el-tag>
              </div>
            </div>
            <div class="task-card__actions">
              <NbButton v-if="task.status === 'pending'" type="success" @click="handleComplete(task.id)">完成</NbButton>
              <NbButton v-else @click="handleReopen(task.id)">重开</NbButton>
              <NbButton v-if="referencePath(task)" @click="router.push(referencePath(task)!)">去处理</NbButton>
            </div>
          </div>
        </NbCard>
      </div>
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
import { useCoachStore } from '@/stores/coach'
import { COACH_PRIORITY_LABELS, COACH_TASK_TYPE_LABELS } from '@/types/coach'
import type { CoachTaskVO } from '@/types/coach'

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
  if (await coachStore.completeTask(id)) ElMessage.success('任务已完成')
}

async function handleReopen(id: number) {
  if (await coachStore.reopenTask(id)) ElMessage.success('任务已重开')
}

onMounted(() => coachStore.fetchPlan(Number(route.params.id)))
</script>

<style scoped>
.plan-page { display: flex; flex-direction: column; gap: 20px; }
.plan-header h1 { margin: 0; font-family: var(--font-heading); }
.plan-header p, .task-card p { color: var(--nb-muted); }
.day-section { display: flex; flex-direction: column; gap: 12px; }
.task-card__main { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.task-card__main h3 { margin: 0; }
.task-card__actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.tags { display: flex; gap: 8px; margin-top: 12px; }
@media (max-width: 768px) { .task-card__main { flex-direction: column; } .task-card__actions { justify-content: flex-start; } }
</style>

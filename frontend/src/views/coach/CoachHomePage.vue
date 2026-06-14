<template>
  <MainLayout>
    <div class="coach-page">
      <NbPageHeader
        eyebrow="AI Coach"
        title="AI 求职教练"
        description="把简历、面试、八股和投递数据整理成今天该做的行动。"
      >
        <template #actions>
          <NbButton variant="primary" @click="showGenerateDialog = true">生成诊断与计划</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="coachStore.loading">
        <NbLoadingBlock title="加载教练数据..." :rows="5" />
      </NbCard>

      <template v-else>
        <NbEmptyState
          v-if="!coachStore.overview?.latestDiagnosis"
          title="还没有求职诊断"
          description="生成第一份诊断后，我会给你一份 7 天行动计划。"
          variant="ai"
        >
          <template #action>
            <NbButton variant="primary" @click="showGenerateDialog = true">开始生成</NbButton>
          </template>
        </NbEmptyState>

        <template v-else>
          <div class="coach-grid">
            <NbCard variant="ai" class="coach-card">
              <NbSectionTitle title="最近诊断" />
              <div class="diagnosis-summary">
                <div class="diagnosis-summary__score">
                  {{ coachStore.overview.latestDiagnosis.overallScore }}
                </div>
                <div class="diagnosis-summary__body">
                  <h3 class="diagnosis-summary__title">{{ coachStore.overview.latestDiagnosis.title }}</h3>
                  <p class="diagnosis-summary__text">{{ coachStore.overview.latestDiagnosis.summary }}</p>
                </div>
              </div>
              <NbButton @click="router.push(`/coach/diagnosis/${coachStore.overview.latestDiagnosis.id}`)">
                查看诊断详情
              </NbButton>
            </NbCard>

            <NbCard v-if="coachStore.overview.activePlan" variant="accent" class="coach-card">
              <NbSectionTitle title="当前 7 天计划" />
              <h3 class="plan-title">{{ coachStore.overview.activePlan.title }}</h3>
              <el-progress :percentage="planProgress" :stroke-width="12" :format="() => planProgressText" />
              <p class="plan-progress-text">
                {{ coachStore.overview.activePlan.completedTaskCount }}/{{ coachStore.overview.activePlan.totalTaskCount }} 个任务已完成
              </p>
              <NbButton @click="router.push(`/coach/plan/${coachStore.overview.activePlan.id}`)">
                查看计划
              </NbButton>
            </NbCard>
          </div>

          <NbCard v-if="coachStore.overview.todayTasks?.length">
            <NbSectionTitle title="今日任务" />
            <div class="task-list">
              <div v-for="task in coachStore.overview.todayTasks" :key="task.id" class="task-row">
                <div class="task-row__info">
                  <span class="task-row__title">{{ task.title }}</span>
                  <span class="task-row__desc">{{ task.description }}</span>
                </div>
                <NbStatusBadge
                  :label="getStatusDescriptor(coachTaskStatusMap, task.status).label"
                  :variant="getStatusDescriptor(coachTaskStatusMap, task.status).variant"
                />
              </div>
            </div>
          </NbCard>

          <div class="history-grid">
            <NbCard>
              <NbSectionTitle title="历史诊断" />
              <NbEmptyState
                v-if="coachStore.diagnoses.length === 0"
                title="暂无历史诊断"
                description="生成的诊断会出现在这里。"
              />
              <div v-else class="history-list">
                <div
                  v-for="diagnosis in coachStore.diagnoses.slice(0, 5)"
                  :key="diagnosis.id"
                  class="history-item"
                  @click="router.push(`/coach/diagnosis/${diagnosis.id}`)"
                >
                  <div class="history-item__info">
                    <span class="history-item__title">{{ diagnosis.title }}</span>
                    <NbStatusBadge :label="`${diagnosis.overallScore} 分`" variant="info" />
                  </div>
                  <span class="history-item__date">{{ formatDate(diagnosis.createTime) }}</span>
                </div>
              </div>
            </NbCard>

            <NbCard>
              <NbSectionTitle title="历史计划" />
              <NbEmptyState
                v-if="coachStore.plans.length === 0"
                title="暂无历史计划"
                description="生成的计划会出现在这里。"
              />
              <div v-else class="history-list">
                <div
                  v-for="plan in coachStore.plans.slice(0, 5)"
                  :key="plan.id"
                  class="history-item"
                  @click="router.push(`/coach/plan/${plan.id}`)"
                >
                  <div class="history-item__info">
                    <span class="history-item__title">{{ plan.title }}</span>
                    <NbStatusBadge
                      :label="COACH_PLAN_STATUS_LABELS[plan.status]"
                      :variant="plan.status === 'active' ? 'primary' : plan.status === 'completed' ? 'success' : 'muted'"
                    />
                  </div>
                  <span class="history-item__date">{{ formatDate(plan.createTime) }}</span>
                </div>
              </div>
            </NbCard>
          </div>
        </template>
      </template>

      <el-dialog v-model="showGenerateDialog" title="生成求职诊断与计划" width="520px">
        <el-form label-width="100px">
          <el-form-item label="目标岗位">
            <el-input v-model="generateForm.targetPosition" placeholder="如：Java 后端开发" />
          </el-form-item>
          <el-form-item label="本次重点">
            <el-input v-model="generateForm.focus" type="textarea" :rows="4" placeholder="如：准备春招、强化 Redis、提升项目表达" />
          </el-form-item>
        </el-form>
        <template #footer>
          <NbButton @click="showGenerateDialog = false">取消</NbButton>
          <NbButton variant="primary" :loading="coachStore.generating" @click="handleGenerate">生成</NbButton>
        </template>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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
import { COACH_PLAN_STATUS_LABELS } from '@/types/coach'
import { coachTaskStatusMap, getStatusDescriptor } from '@/utils/statusMaps'
import { formatDate } from '@/utils/date'

const router = useRouter()
const coachStore = useCoachStore()
const showGenerateDialog = ref(false)
const generateForm = reactive({ targetPosition: '', focus: '' })

const planProgress = computed(() => {
  const plan = coachStore.overview?.activePlan
  if (!plan || plan.totalTaskCount === 0) return 0
  return Math.round((plan.completedTaskCount / plan.totalTaskCount) * 100)
})

const planProgressText = computed(() => {
  const plan = coachStore.overview?.activePlan
  if (!plan) return ''
  return `${plan.completedTaskCount}/${plan.totalTaskCount}`
})

async function handleGenerate() {
  const result = await coachStore.generate({ ...generateForm })
  if (result) {
    ElMessage.success('诊断与计划已生成')
    showGenerateDialog.value = false
    await Promise.all([coachStore.fetchDiagnoses(), coachStore.fetchPlans()])
  } else {
    ElMessage.error('生成失败，请重试')
  }
}

onMounted(async () => {
  await Promise.all([coachStore.fetchOverview(), coachStore.fetchDiagnoses(), coachStore.fetchPlans()])
})
</script>

<style scoped>
.coach-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.coach-grid,
.history-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.coach-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.diagnosis-summary {
  display: flex;
  align-items: center;
  gap: 20px;
}

.diagnosis-summary__score {
  flex-shrink: 0;
  width: 84px;
  height: 84px;
  display: grid;
  place-items: center;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  background: var(--nb-primary);
  color: #fff;
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 800;
  border-radius: var(--nb-radius-lg);
}

.diagnosis-summary__body {
  min-width: 0;
}

.diagnosis-summary__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
}

.diagnosis-summary__text {
  margin: 8px 0 0;
  color: var(--nb-muted);
  font-size: 14px;
  line-height: 1.5;
}

.plan-title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
}

.plan-progress-text {
  margin: 0;
  color: var(--nb-muted);
  font-size: 14px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.task-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
}

.task-row__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.task-row__title {
  font-weight: 600;
  font-size: 14px;
}

.task-row__desc {
  color: var(--nb-muted);
  font-size: 13px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
  cursor: pointer;
  transition: var(--nb-transition);
}

.history-item:hover {
  box-shadow: var(--nb-shadow-sm);
  transform: translate(-1px, -1px);
}

.history-item__info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.history-item__title {
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item__date {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--nb-muted);
}

@media (max-width: 768px) {
  .coach-grid,
  .history-grid {
    grid-template-columns: 1fr;
  }
}
</style>

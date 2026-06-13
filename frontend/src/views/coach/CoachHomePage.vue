<template>
  <MainLayout>
    <div class="coach-page">
      <div class="coach-page__header">
        <div>
          <h1 class="coach-page__title">AI 求职教练</h1>
          <p class="coach-page__subtitle">把简历、面试、八股和投递数据整理成今天该做的行动。</p>
        </div>
        <NbButton type="primary" @click="showGenerateDialog = true">生成诊断与计划</NbButton>
      </div>

      <div v-if="coachStore.loading" class="coach-page__loading">加载中...</div>

      <template v-else>
        <NbCard v-if="!coachStore.overview?.latestDiagnosis" class="coach-card coach-card--empty">
          <h2>还没有求职诊断</h2>
          <p>生成第一份诊断后，我会给你一份 7 天行动计划。</p>
          <NbButton type="primary" @click="showGenerateDialog = true">开始生成</NbButton>
        </NbCard>

        <div v-else class="coach-grid">
          <NbCard class="coach-card">
            <h2>最近诊断</h2>
            <div class="score">{{ coachStore.overview.latestDiagnosis.overallScore }}</div>
            <h3>{{ coachStore.overview.latestDiagnosis.title }}</h3>
            <p>{{ coachStore.overview.latestDiagnosis.summary }}</p>
            <router-link :to="`/coach/diagnosis/${coachStore.overview.latestDiagnosis.id}`">
              <NbButton>查看诊断详情</NbButton>
            </router-link>
          </NbCard>

          <NbCard v-if="coachStore.overview.activePlan" class="coach-card">
            <h2>当前 7 天计划</h2>
            <h3>{{ coachStore.overview.activePlan.title }}</h3>
            <el-progress :percentage="planProgress" :stroke-width="12" />
            <p>{{ coachStore.overview.activePlan.completedTaskCount }}/{{ coachStore.overview.activePlan.totalTaskCount }} 个任务已完成</p>
            <router-link :to="`/coach/plan/${coachStore.overview.activePlan.id}`">
              <NbButton>查看计划</NbButton>
            </router-link>
          </NbCard>
        </div>

        <NbCard v-if="coachStore.overview?.todayTasks?.length" class="coach-card">
          <h2>今日任务</h2>
          <div v-for="task in coachStore.overview.todayTasks" :key="task.id" class="task-row">
            <div>
              <strong>{{ task.title }}</strong>
              <p>{{ task.description }}</p>
            </div>
            <el-tag :type="task.status === 'completed' ? 'success' : 'warning'">{{ task.status === 'completed' ? '已完成' : '待完成' }}</el-tag>
          </div>
        </NbCard>

        <div class="history-grid">
          <NbCard class="coach-card">
            <h2>历史诊断</h2>
            <div v-for="diagnosis in coachStore.diagnoses.slice(0, 5)" :key="diagnosis.id" class="history-row" @click="router.push(`/coach/diagnosis/${diagnosis.id}`)">
              <span>{{ diagnosis.title }}</span>
              <span>{{ formatDate(diagnosis.createTime) }}</span>
            </div>
          </NbCard>
          <NbCard class="coach-card">
            <h2>历史计划</h2>
            <div v-for="plan in coachStore.plans.slice(0, 5)" :key="plan.id" class="history-row" @click="router.push(`/coach/plan/${plan.id}`)">
              <span>{{ plan.title }}</span>
              <el-tag size="small">{{ COACH_PLAN_STATUS_LABELS[plan.status] }}</el-tag>
            </div>
          </NbCard>
        </div>
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
          <NbButton type="primary" :loading="coachStore.generating" @click="handleGenerate">生成</NbButton>
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
import { useCoachStore } from '@/stores/coach'
import { COACH_PLAN_STATUS_LABELS } from '@/types/coach'

const router = useRouter()
const coachStore = useCoachStore()
const showGenerateDialog = ref(false)
const generateForm = reactive({ targetPosition: '', focus: '' })

const planProgress = computed(() => {
  const plan = coachStore.overview?.activePlan
  if (!plan || plan.totalTaskCount === 0) return 0
  return Math.round((plan.completedTaskCount / plan.totalTaskCount) * 100)
})

function formatDate(value: string) {
  return new Date(value).toLocaleDateString()
}

async function handleGenerate() {
  const result = await coachStore.generate({ ...generateForm })
  if (result) {
    ElMessage.success('诊断与计划已生成')
    showGenerateDialog.value = false
    await Promise.all([coachStore.fetchDiagnoses(), coachStore.fetchPlans()])
  }
}

onMounted(async () => {
  await Promise.all([coachStore.fetchOverview(), coachStore.fetchDiagnoses(), coachStore.fetchPlans()])
})
</script>

<style scoped>
.coach-page { display: flex; flex-direction: column; gap: 24px; }
.coach-page__header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.coach-page__title { margin: 0; font-family: var(--font-heading); font-size: 36px; }
.coach-page__subtitle { margin: 8px 0 0; color: var(--nb-muted); }
.coach-page__loading { padding: 48px; text-align: center; }
.coach-grid, .history-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.coach-card { display: flex; flex-direction: column; gap: 12px; }
.coach-card--empty { align-items: flex-start; }
.score { width: 84px; height: 84px; display: grid; place-items: center; border: var(--nb-border); box-shadow: var(--nb-shadow); background: var(--nb-primary); color: white; font-size: 32px; font-weight: 800; }
.task-row, .history-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 12px; border: var(--nb-border); background: #fff; cursor: pointer; }
.task-row p { margin: 4px 0 0; color: var(--nb-muted); }
@media (max-width: 768px) { .coach-grid, .history-grid { grid-template-columns: 1fr; } .coach-page__header { align-items: flex-start; flex-direction: column; } }
</style>

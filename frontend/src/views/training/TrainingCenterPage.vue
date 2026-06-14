<template>
  <MainLayout>
    <div class="training-center">
      <NbPageHeader
        eyebrow="训练中心"
        title="八股训练"
        description="生成个性化训练计划，按天推进面试八股练习，AI 批改追踪掌握进度"
      >
        <template #actions>
          <NbButton variant="primary" @click="showGenDialog = true">生成新计划</NbButton>
        </template>
      </NbPageHeader>

      <NbCard>
        <NbSectionTitle title="复习工具" />
        <div class="training-center__tools">
          <NbButton @click="router.push('/training/mistakes')">错题本</NbButton>
          <NbButton @click="router.push('/training/mastery')">知识点掌握度</NbButton>
        </div>
      </NbCard>

      <NbCard v-if="trainingStore.loading">
        <NbLoadingBlock title="加载训练计划..." :rows="4" />
      </NbCard>

      <template v-else>
        <NbCard v-if="trainingStore.activePlan" variant="accent">
          <NbSectionTitle title="当前训练计划">
            <template #actions>
              <NbButton @click="router.push(`/training/plan/${trainingStore.activePlan.id}`)">查看详情</NbButton>
            </template>
          </NbSectionTitle>
          <div class="active-plan">
            <h4 class="active-plan__title">{{ trainingStore.activePlan.title }}</h4>
            <p v-if="trainingStore.activePlan.summary" class="active-plan__summary">
              {{ trainingStore.activePlan.summary }}
            </p>
            <el-progress
              :percentage="activePlanProgress"
              :stroke-width="12"
              :format="() => activePlanProgressText"
            />
            <div v-if="trainingStore.activePlan.focusTopics.length > 0" class="active-plan__topics">
              <NbStatusBadge
                v-for="topic in trainingStore.activePlan.focusTopics"
                :key="topic"
                :label="topic"
                variant="info"
              />
            </div>
          </div>
        </NbCard>

        <NbCard v-else>
          <NbEmptyState
            title="暂无进行中的训练计划"
            description="点击上方「生成新计划」按钮，AI 将根据你的简历或职位生成个性化八股练习计划"
          >
            <template #action>
              <NbButton variant="primary" @click="showGenDialog = true">生成新计划</NbButton>
            </template>
          </NbEmptyState>
        </NbCard>

        <NbCard
          v-if="trainingStore.activePlan && groupedQuestions.length > 0"
        >
          <NbSectionTitle title="题目列表" />
          <div
            v-for="group in groupedQuestions"
            :key="group.dayIndex"
            class="day-group"
          >
            <h4 class="day-group__title">Day {{ group.dayIndex }}</h4>
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
                </div>
                <NbStatusBadge
                  :label="getStatusDescriptor(trainingQuestionStatusMap, q.status).label"
                  :variant="getStatusDescriptor(trainingQuestionStatusMap, q.status).variant"
                />
              </div>
            </div>
          </div>
        </NbCard>

        <NbCard
          v-if="trainingStore.activePlan && trainingStore.activePlan.algorithmRecommendations.length > 0"
        >
          <NbSectionTitle title="算法推荐" />
          <div class="algo-list">
            <NbCard
              v-for="algo in trainingStore.activePlan.algorithmRecommendations"
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

        <NbCard v-if="historyPlans.length > 0">
          <NbSectionTitle title="历史计划" />
          <div class="history-list">
            <div
              v-for="plan in historyPlans"
              :key="plan.id"
              class="history-item"
              @click="router.push(`/training/plan/${plan.id}`)"
            >
              <div class="history-item__info">
                <span class="history-item__title">{{ plan.title }}</span>
                <NbStatusBadge
                  :label="plan.status === 'completed' ? '已完成' : '已归档'"
                  :variant="plan.status === 'completed' ? 'success' : 'muted'"
                />
              </div>
              <span class="history-item__date">{{ formatDate(plan.createTime) }}</span>
            </div>
          </div>
        </NbCard>
      </template>

      <el-dialog v-model="showGenDialog" title="生成训练计划" width="480px">
        <el-form label-width="100px" @submit.prevent="handleGenerate">
          <el-form-item label="目标天数">
            <el-input-number v-model="genForm.targetDays" :min="1" :max="30" />
          </el-form-item>
          <el-form-item label="目标岗位">
            <el-input v-model="genForm.targetPosition" placeholder="如：Java 后端开发" />
          </el-form-item>
          <el-form-item label="来源类型">
            <el-select v-model="genForm.sourceType" placeholder="选择来源" clearable>
              <el-option label="自动" value="auto" />
              <el-option label="简历" value="resume" />
              <el-option label="职位" value="job" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <NbButton @click="showGenDialog = false">取消</NbButton>
          <NbButton type="primary" :loading="generating" @click="handleGenerate">生成</NbButton>
        </template>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
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
import type { TrainingQuestionVO } from '@/types/training'
import {
  trainingQuestionStatusMap,
  trainingDifficultyMap,
  getStatusDescriptor,
} from '@/utils/statusMaps'
import { formatDate } from '@/utils/date'

const router = useRouter()
const trainingStore = useTrainingStore()

const showGenDialog = ref(false)
const generating = ref(false)
const genForm = reactive({
  targetDays: 7,
  targetPosition: '',
  sourceType: 'auto',
})

const groupedQuestions = computed(() => {
  const plan = trainingStore.activePlan
  if (!plan) return []
  const map = new Map<number, TrainingQuestionVO[]>()
  for (const q of plan.questions) {
    const list = map.get(q.dayIndex) || []
    list.push(q)
    map.set(q.dayIndex, list)
  }
  return Array.from(map.entries())
    .sort(([a], [b]) => a - b)
    .map(([dayIndex, questions]) => ({ dayIndex, questions }))
})

const historyPlans = computed(() =>
  trainingStore.plans.filter((p) => p.status === 'completed' || p.status === 'archived'),
)

const activePlanProgress = computed(() => {
  const plan = trainingStore.activePlan
  if (!plan || plan.questions.length === 0) return 0
  const reviewed = plan.questions.filter(
    (q) => q.status === 'reviewed' || q.status === 'mastered',
  ).length
  return Math.round((reviewed / plan.questions.length) * 100)
})

const activePlanProgressText = computed(() => {
  const plan = trainingStore.activePlan
  if (!plan) return ''
  const reviewed = plan.questions.filter(
    (q) => q.status === 'reviewed' || q.status === 'mastered',
  ).length
  return `${reviewed}/${plan.questions.length}`
})

async function handleGenerate() {
  generating.value = true
  try {
    const result = await trainingStore.generatePlan({
      targetDays: genForm.targetDays,
      targetPosition: genForm.targetPosition || undefined,
      sourceType: genForm.sourceType || undefined,
    })
    if (result) {
      ElMessage.success('训练计划已生成')
      showGenDialog.value = false
      await trainingStore.fetchActivePlan()
      await trainingStore.fetchPlans()
    } else {
      ElMessage.error('生成失败，请重试')
    }
  } finally {
    generating.value = false
  }
}

async function handleCompleteAlgo(id: number) {
  const ok = await trainingStore.completeAlgorithm(id)
  if (ok) {
    ElMessage.success('已标记完成')
    trainingStore.fetchActivePlan()
  }
}

async function handleReopenAlgo(id: number) {
  const ok = await trainingStore.reopenAlgorithm(id)
  if (ok) {
    ElMessage.success('已重新打开')
    trainingStore.fetchActivePlan()
  }
}

onMounted(() => {
  trainingStore.fetchActivePlan()
  trainingStore.fetchPlans()
})
</script>

<style scoped>
.training-center {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.training-center__tools {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.active-plan {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.active-plan__title {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.active-plan__summary {
  color: var(--nb-muted);
  font-size: 14px;
  margin: 0;
  line-height: 1.5;
}

.active-plan__topics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.day-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.day-group:last-child {
  margin-bottom: 0;
}

.day-group__title {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  color: var(--nb-primary);
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

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.history-item {
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

.history-item:hover {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-1px, -1px);
}

.history-item__info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.history-item__title {
  font-weight: 500;
  font-size: 14px;
}

.history-item__date {
  font-size: 13px;
  color: var(--nb-muted);
}
</style>

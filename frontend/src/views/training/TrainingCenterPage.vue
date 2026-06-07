<!-- src/views/training/TrainingCenterPage.vue -->
<template>
  <MainLayout>
    <div class="training-center">
      <div class="training-center__header">
        <h1 class="training-center__title">训练中心</h1>
        <NbButton type="primary" @click="showGenDialog = true">生成新计划</NbButton>
      </div>

      <div v-if="trainingStore.loading" class="training-center__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else>
        <NbCard v-if="trainingStore.activePlan" class="section-card">
          <h3 class="section-title">当前训练计划</h3>
          <div class="active-plan">
            <div class="active-plan__header">
              <h4 class="active-plan__title">{{ trainingStore.activePlan.title }}</h4>
              <router-link :to="`/training/plan/${trainingStore.activePlan.id}`">
                <NbButton>查看详情</NbButton>
              </router-link>
            </div>
            <p v-if="trainingStore.activePlan.summary" class="active-plan__summary">
              {{ trainingStore.activePlan.summary }}
            </p>
            <el-progress
              :percentage="activePlanProgress"
              :stroke-width="12"
              :format="activePlanProgressFormat"
            />
            <div v-if="trainingStore.activePlan.focusTopics.length > 0" class="tag-cloud">
              <el-tag
                v-for="topic in trainingStore.activePlan.focusTopics"
                :key="topic"
                size="small"
                class="nb-tag"
              >
                {{ topic }}
              </el-tag>
            </div>
          </div>
        </NbCard>

        <NbCard v-else class="section-card">
          <h3 class="section-title">当前训练计划</h3>
          <p class="empty-hint">暂无进行中的训练计划，点击上方按钮生成新计划</p>
        </NbCard>

        <NbCard
          v-if="trainingStore.activePlan && groupedQuestions.length > 0"
          class="section-card"
        >
          <h3 class="section-title">题目列表</h3>
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
                  <el-tag size="small" class="nb-tag">{{ q.topic }}</el-tag>
                  <el-tag
                    :type="difficultyTagType(q.difficulty)"
                    size="small"
                    class="nb-tag"
                  >
                    {{ difficultyLabel(q.difficulty) }}
                  </el-tag>
                </div>
                <el-tag
                  :type="questionStatusTagType(q.status)"
                  size="small"
                  class="nb-tag"
                >
                  {{ QUESTION_STATUS_LABELS[q.status] }}
                </el-tag>
              </div>
            </div>
          </div>
        </NbCard>

        <NbCard
          v-if="trainingStore.activePlan && trainingStore.activePlan.algorithmRecommendations.length > 0"
          class="section-card"
        >
          <h3 class="section-title">算法推荐</h3>
          <div class="algo-list">
            <div
              v-for="algo in trainingStore.activePlan.algorithmRecommendations"
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

        <NbCard
          v-if="historyPlans.length > 0"
          class="section-card"
        >
          <h3 class="section-title">历史计划</h3>
          <div class="history-list">
            <div
              v-for="plan in historyPlans"
              :key="plan.id"
              class="history-item"
              @click="router.push(`/training/plan/${plan.id}`)"
            >
              <div class="history-item__info">
                <span class="history-item__title">{{ plan.title }}</span>
                <el-tag
                  :type="plan.status === 'completed' ? 'success' : 'info'"
                  size="small"
                  class="nb-tag"
                >
                  {{ plan.status === 'completed' ? '已完成' : '已归档' }}
                </el-tag>
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
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useTrainingStore } from '@/stores/training'
import {
  QUESTION_STATUS_LABELS,
  TRAINING_DIFFICULTY_OPTIONS,
} from '@/types/training'
import type { TrainingQuestionStatus, TrainingDifficulty, TrainingQuestionVO } from '@/types/training'

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

function activePlanProgressFormat(percentage: number) {
  const plan = trainingStore.activePlan
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

function difficultyTagType(difficulty: TrainingDifficulty) {
  if (difficulty === 'hard') return 'danger'
  if (difficulty === 'medium') return 'warning'
  return 'success'
}

function difficultyLabel(difficulty: TrainingDifficulty) {
  return TRAINING_DIFFICULTY_OPTIONS.find((o) => o.value === difficulty)?.label || difficulty
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

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

.training-center__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.training-center__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.training-center__loading {
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

.active-plan {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.active-plan__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.tag-cloud {
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
  margin: 0;
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

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
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

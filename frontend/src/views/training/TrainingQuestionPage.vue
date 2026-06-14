<template>
  <MainLayout>
    <div class="tq-page">
      <NbCard v-if="trainingStore.loading && !question">
        <NbLoadingBlock title="加载题目..." :rows="4" />
      </NbCard>

      <template v-else-if="question">
        <div class="tq-page__back">
          <NbButton variant="ghost" @click="goBack">&larr; 返回训练计划</NbButton>
        </div>

        <NbPageHeader :title="question.title">
          <template #actions>
            <NbButton variant="success" :loading="actionLoading === 'master'" @click="handleMaster">标记已掌握</NbButton>
            <NbButton variant="secondary" :loading="actionLoading === 'skip'" @click="handleSkip">跳过</NbButton>
          </template>
        </NbPageHeader>

        <div class="tq-page__tags">
          <NbStatusBadge :label="question.topic" variant="info" />
          <NbStatusBadge
            :label="getStatusDescriptor(trainingDifficultyMap, question.difficulty).label"
            :variant="getStatusDescriptor(trainingDifficultyMap, question.difficulty).variant"
          />
          <NbStatusBadge :label="question.sourceType" variant="default" />
          <NbStatusBadge
            :label="getStatusDescriptor(trainingQuestionStatusMap, question.status).label"
            :variant="getStatusDescriptor(trainingQuestionStatusMap, question.status).variant"
          />
          <template v-if="question.skillTags?.length">
            <NbStatusBadge
              v-for="tag in question.skillTags"
              :key="tag"
              :label="tag"
              variant="muted"
            />
          </template>
        </div>

        <NbCard>
          <NbSectionTitle title="题目内容" />
          <div class="tq-page__content nb-prewrap">{{ question.content }}</div>
          <el-collapse v-if="question.referenceAnswer" class="tq-page__ref-collapse">
            <el-collapse-item title="参考答案" name="ref">
              <div class="tq-page__ref-answer nb-prewrap">{{ question.referenceAnswer }}</div>
            </el-collapse-item>
          </el-collapse>
        </NbCard>

        <NbCard v-if="!latestReview">
          <NbSectionTitle title="作答" />
          <el-input
            v-model="answerText"
            type="textarea"
            placeholder="请输入你的八股答案..."
            :rows="8"
            :maxlength="8000"
            show-word-limit
          />
          <div class="tq-page__submit-row">
            <NbButton
              variant="primary"
              :loading="submitting"
              :disabled="!answerText.trim()"
              @click="handleSubmit"
            >
              提交答案
            </NbButton>
          </div>
        </NbCard>

        <NbCard v-if="latestReview" variant="ai">
          <NbSectionTitle title="AI 批改结果" />

          <div class="tq-page__score-row">
            <div class="tq-page__total-score">
              <span class="tq-page__total-score-num">{{ latestReview.totalScore }}</span>
              <span class="tq-page__total-score-label">总分</span>
            </div>
            <div class="tq-page__dim-scores">
              <div class="tq-page__dim-item">
                <span class="tq-page__dim-label">准确性</span>
                <el-progress :percentage="latestReview.accuracyScore" :stroke-width="10" :color="'#6C5CE7'" />
              </div>
              <div class="tq-page__dim-item">
                <span class="tq-page__dim-label">清晰度</span>
                <el-progress :percentage="latestReview.clarityScore" :stroke-width="10" :color="'#00CEC9'" />
              </div>
              <div class="tq-page__dim-item">
                <span class="tq-page__dim-label">深度</span>
                <el-progress :percentage="latestReview.depthScore" :stroke-width="10" :color="'#FD79A8'" />
              </div>
              <div class="tq-page__dim-item">
                <span class="tq-page__dim-label">项目关联</span>
                <el-progress :percentage="latestReview.projectScore" :stroke-width="10" :color="'#00B894'" />
              </div>
            </div>
          </div>

          <div class="tq-page__mastery-row">
            <span>掌握程度：</span>
            <NbStatusBadge
              :label="getStatusDescriptor(trainingMasteryMap, latestReview.masteryLevel).label"
              :variant="getStatusDescriptor(trainingMasteryMap, latestReview.masteryLevel).variant"
            />
          </div>

          <div v-if="latestReview.strengths?.length" class="tq-page__review-section">
            <h4 class="tq-page__review-heading tq-page__review-heading--green">优点</h4>
            <ul class="tq-page__review-list">
              <li v-for="(item, i) in latestReview.strengths" :key="i">
                <span class="tq-page__icon tq-page__icon--green">&#10003;</span>
                {{ item }}
              </li>
            </ul>
          </div>

          <div v-if="latestReview.mistakes?.length" class="tq-page__review-section">
            <h4 class="tq-page__review-heading tq-page__review-heading--red">错误</h4>
            <ul class="tq-page__review-list">
              <li v-for="(item, i) in latestReview.mistakes" :key="i">
                <span class="tq-page__icon tq-page__icon--red">&#10007;</span>
                {{ item }}
              </li>
            </ul>
          </div>

          <div v-if="latestReview.missingPoints?.length" class="tq-page__review-section">
            <h4 class="tq-page__review-heading tq-page__review-heading--orange">遗漏要点</h4>
            <ul class="tq-page__review-list">
              <li v-for="(item, i) in latestReview.missingPoints" :key="i">
                <span class="tq-page__icon tq-page__icon--orange">&#9888;</span>
                {{ item }}
              </li>
            </ul>
          </div>

          <div v-if="latestReview.suggestions?.length" class="tq-page__review-section">
            <h4 class="tq-page__review-heading tq-page__review-heading--blue">改进建议</h4>
            <ul class="tq-page__review-list">
              <li v-for="(item, i) in latestReview.suggestions" :key="i">
                <span class="tq-page__icon tq-page__icon--blue">&#8505;</span>
                {{ item }}
              </li>
            </ul>
          </div>

          <div v-if="latestReview.recommendedAnswer" class="tq-page__review-section">
            <h4 class="tq-page__review-heading">推荐答案</h4>
            <div class="tq-page__rec-answer nb-prewrap">{{ latestReview.recommendedAnswer }}</div>
          </div>

          <div v-if="latestReview.followUpQuestions?.length" class="tq-page__review-section">
            <h4 class="tq-page__review-heading">追问</h4>
            <ol class="tq-page__followup-list">
              <li v-for="(q, i) in latestReview.followUpQuestions" :key="i">{{ q }}</li>
            </ol>
          </div>
        </NbCard>

        <NbCard v-if="trainingStore.answers.length > 0">
          <NbSectionTitle title="历史作答" />
          <div class="tq-page__history-list">
            <div v-for="ans in trainingStore.answers" :key="ans.id" class="tq-page__history-item">
              <div class="tq-page__history-meta">
                <span class="tq-page__history-time">{{ formatDateTime(ans.createTime) }}</span>
                <NbStatusBadge
                  v-if="ans.review"
                  :label="getStatusDescriptor(trainingMasteryMap, ans.review.masteryLevel).label"
                  :variant="getStatusDescriptor(trainingMasteryMap, ans.review.masteryLevel).variant"
                />
                <span v-if="ans.review" class="tq-page__history-score">{{ ans.review.totalScore }} 分</span>
              </div>
              <div class="tq-page__history-preview nb-prewrap">{{ ans.answerText }}</div>
            </div>
          </div>
        </NbCard>
      </template>

      <NbCard v-else>
        <NbEmptyState title="未找到该题目">
          <template #action>
            <NbButton variant="primary" @click="router.push('/training')">返回训练中心</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
import {
  trainingMasteryMap,
  trainingQuestionStatusMap,
  trainingDifficultyMap,
  getStatusDescriptor,
} from '@/utils/statusMaps'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const trainingStore = useTrainingStore()

const answerText = ref('')
const submitting = ref(false)
const actionLoading = ref<string | null>(null)

const question = computed(() => trainingStore.currentQuestion)
const latestReview = computed(() => {
  const answers = trainingStore.answers
  for (let i = answers.length - 1; i >= 0; i--) {
    const r = answers[i]!.review
    if (r) return r
  }
  return null
})

const questionId = computed(() => Number(route.params.id))

onMounted(() => {
  trainingStore.fetchQuestion(questionId.value)
  trainingStore.fetchAnswers(questionId.value)
})

function goBack() {
  if (question.value?.planId) {
    router.push(`/training/plan/${question.value.planId}`)
  } else {
    router.push('/training')
  }
}

async function handleSubmit() {
  if (!answerText.value.trim()) return
  submitting.value = true
  try {
    const result = await trainingStore.submitAnswer(questionId.value, {
      answerText: answerText.value,
    })
    if (result) {
      ElMessage.success('提交成功')
      answerText.value = ''
      trainingStore.fetchQuestion(questionId.value)
      trainingStore.fetchAnswers(questionId.value)
    } else {
      ElMessage.error('提交失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleMaster() {
  actionLoading.value = 'master'
  try {
    const ok = await trainingStore.masterQuestion(questionId.value)
    if (ok) {
      ElMessage.success('已标记为已掌握')
      trainingStore.fetchQuestion(questionId.value)
    } else {
      ElMessage.error('操作失败')
    }
  } finally {
    actionLoading.value = null
  }
}

async function handleSkip() {
  actionLoading.value = 'skip'
  try {
    const ok = await trainingStore.skipQuestion(questionId.value)
    if (ok) {
      ElMessage.success('已跳过')
      trainingStore.fetchQuestion(questionId.value)
    } else {
      ElMessage.error('操作失败')
    }
  } finally {
    actionLoading.value = null
  }
}
</script>

<style scoped>
.tq-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tq-page__back {
  display: flex;
  align-items: center;
}

.tq-page__tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tq-page__content {
  line-height: 1.8;
  margin-top: 16px;
}

.tq-page__ref-collapse {
  margin-top: 16px;
}

.tq-page__ref-answer {
  line-height: 1.8;
  color: var(--nb-muted);
  background: var(--nb-muted-surface);
  padding: 12px;
  border-radius: var(--nb-radius);
  border: var(--nb-border);
}

.tq-page__submit-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.tq-page__score-row {
  display: flex;
  gap: 32px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 16px;
  margin-bottom: 20px;
}

.tq-page__total-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 100px;
  padding: 16px;
  border: var(--nb-border);
  border-radius: var(--nb-radius-lg);
  box-shadow: var(--nb-shadow);
  background: var(--nb-surface);
}

.tq-page__total-score-num {
  font-family: var(--font-heading);
  font-size: 40px;
  font-weight: 700;
  color: var(--nb-primary);
  line-height: 1;
}

.tq-page__total-score-label {
  font-size: 13px;
  color: var(--nb-muted);
  margin-top: 4px;
}

.tq-page__dim-scores {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 240px;
}

.tq-page__dim-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tq-page__dim-label {
  font-size: 14px;
  font-weight: 500;
  min-width: 70px;
}

.tq-page__dim-item :deep(.el-progress) {
  flex: 1;
}

.tq-page__mastery-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-weight: 500;
}

.tq-page__review-section {
  margin-bottom: 20px;
}

.tq-page__review-heading {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.tq-page__review-heading--green { color: var(--nb-success); }
.tq-page__review-heading--red { color: var(--nb-danger); }
.tq-page__review-heading--orange { color: var(--nb-warning); }
.tq-page__review-heading--blue { color: var(--nb-secondary); }

.tq-page__review-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tq-page__review-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.6;
}

.tq-page__icon {
  flex-shrink: 0;
  font-weight: 700;
  font-size: 14px;
  margin-top: 2px;
}

.tq-page__icon--green { color: var(--nb-success); }
.tq-page__icon--red { color: var(--nb-danger); }
.tq-page__icon--orange { color: var(--nb-warning); }
.tq-page__icon--blue { color: var(--nb-secondary); }

.tq-page__rec-answer {
  background: var(--nb-muted-surface);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  padding: 16px;
  line-height: 1.8;
  box-shadow: var(--nb-shadow-xs);
}

.tq-page__followup-list {
  padding-left: 20px;
  margin: 0;
  line-height: 2;
}

.tq-page__history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.tq-page__history-item {
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  padding: 12px;
  background: var(--nb-surface);
}

.tq-page__history-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.tq-page__history-time {
  font-size: 13px;
  color: var(--nb-muted);
}

.tq-page__history-score {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 13px;
  color: var(--nb-primary);
}

.tq-page__history-preview {
  font-size: 14px;
  color: var(--nb-text);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
</style>

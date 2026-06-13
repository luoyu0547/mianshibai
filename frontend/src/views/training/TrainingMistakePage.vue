<template>
  <MainLayout>
    <div class="tm-page">
      <div class="tm-page__header">
        <h2 class="tm-page__title">错题本</h2>
      </div>

      <NbCard class="tm-page__filter-card">
        <div class="tm-page__filter-row">
          <el-input
            v-model="filters.topic"
            placeholder="搜索知识点"
            clearable
            class="tm-page__filter-input"
          />
          <el-select
            v-model="filters.masteryLevel"
            placeholder="掌握程度"
            clearable
            class="tm-page__filter-select"
          >
            <el-option
              v-for="opt in masteryFilterOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-tooltip content="包含已掌握的题目" placement="top">
            <div class="tm-page__switch-group">
              <span class="tm-page__switch-label">含已掌握</span>
              <el-switch v-model="filters.includeMastered" />
            </div>
          </el-tooltip>
          <el-select
            v-model="filters.scoreMax"
            placeholder="最高得分"
            clearable
            class="tm-page__filter-select"
          >
            <el-option label="60 分以下" :value="60" />
            <el-option label="70 分以下" :value="70" />
            <el-option label="80 分以下" :value="80" />
          </el-select>
          <NbButton type="primary" @click="handleFilter">筛选</NbButton>
          <NbButton type="secondary" @click="handleReset">重置</NbButton>
        </div>
      </NbCard>

      <div class="tm-page__summary-row">
        <NbCard class="tm-page__summary-card">
          <div class="tm-page__summary-value">{{ trainingStore.mistakes.length }}</div>
          <div class="tm-page__summary-label">全部错题</div>
        </NbCard>
        <NbCard class="tm-page__summary-card">
          <div class="tm-page__summary-value tm-page__summary-value--danger">{{ weakCount }}</div>
          <div class="tm-page__summary-label">薄弱</div>
        </NbCard>
        <NbCard class="tm-page__summary-card">
          <div class="tm-page__summary-value tm-page__summary-value--warning">{{ basicCount }}</div>
          <div class="tm-page__summary-label">基础</div>
        </NbCard>
        <NbCard class="tm-page__summary-card">
          <div class="tm-page__summary-value tm-page__summary-value--success">{{ goodCount }}</div>
          <div class="tm-page__summary-label">良好</div>
        </NbCard>
      </div>

      <template v-if="trainingStore.mistakes.length > 0">
        <div v-for="mistake in trainingStore.mistakes" :key="mistake.questionId" class="tm-page__mistake-item">
          <NbCard class="tm-page__mistake-card">
            <div class="tm-page__mistake-header">
              <router-link
                :to="`/training/question/${mistake.questionId}`"
                class="tm-page__mistake-title"
              >
                {{ mistake.title }}
              </router-link>
              <div class="tm-page__mistake-tags">
                <el-tag effect="dark" class="tm-page__tag">{{ mistake.topic }}</el-tag>
                <el-tag
                  :type="difficultyTagType(mistake.difficulty)"
                  effect="dark"
                  class="tm-page__tag"
                >
                  {{ difficultyLabel(mistake.difficulty) }}
                </el-tag>
                <template v-if="mistake.skillTags?.length">
                  <el-tag
                    v-for="tag in mistake.skillTags"
                    :key="tag"
                    size="small"
                    effect="plain"
                    class="tm-page__tag tm-page__skill-tag"
                  >
                    {{ tag }}
                  </el-tag>
                </template>
                <el-tag
                  effect="dark"
                  :color="masteryColor(mistake.masteryLevel)"
                  class="tm-page__tag"
                  style="border: none; color: #fff;"
                >
                  {{ masteryLabel(mistake.masteryLevel) }}
                </el-tag>
                <el-tag
                  :color="masteryColor(mistake.masteryLevel)"
                  effect="dark"
                  class="tm-page__tag"
                  style="border: none; color: #fff;"
                >
                  {{ mistake.latestScore }} 分
                </el-tag>
                <el-tag :type="statusTagType(mistake.status)" effect="dark" class="tm-page__tag">
                  {{ QUESTION_STATUS_LABELS[mistake.status] }}
                </el-tag>
              </div>
            </div>

            <div v-if="mistake.mistakes?.length" class="tm-page__section">
              <h4 class="tm-page__section-heading tm-page__section-heading--red">错误</h4>
              <ul class="tm-page__section-list">
                <li v-for="(item, i) in mistake.mistakes" :key="i">{{ item }}</li>
              </ul>
            </div>

            <div v-if="mistake.missingPoints?.length" class="tm-page__section">
              <h4 class="tm-page__section-heading tm-page__section-heading--orange">遗漏要点</h4>
              <ul class="tm-page__section-list">
                <li v-for="(item, i) in mistake.missingPoints" :key="i">{{ item }}</li>
              </ul>
            </div>

            <div v-if="mistake.suggestions?.length" class="tm-page__section">
              <h4 class="tm-page__section-heading tm-page__section-heading--blue">改进建议</h4>
              <ul class="tm-page__section-list">
                <li v-for="(item, i) in mistake.suggestions" :key="i">{{ item }}</li>
              </ul>
            </div>

            <div v-if="mistake.recommendedAnswer" class="tm-page__section">
              <h4 class="tm-page__section-heading">推荐答案</h4>
              <p class="tm-page__rec-answer">{{ mistake.recommendedAnswer.slice(0, 200) }}{{ mistake.recommendedAnswer.length > 200 ? '...' : '' }}</p>
            </div>

            <div class="tm-page__mistake-footer">
              <NbButton type="success" @click="handleMaster(mistake.questionId)">标记已掌握</NbButton>
            </div>
          </NbCard>
        </div>
      </template>

      <template v-else-if="!trainingStore.loading">
        <NbCard>
          <div class="tm-page__empty">
            <p>暂无错题，继续完成八股训练后这里会自动出现需要复习的题目。</p>
          </div>
        </NbCard>
      </template>

      <div v-if="trainingStore.loading" class="tm-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useTrainingStore } from '@/stores/training'
import {
  MASTERY_LEVEL_OPTIONS,
  TRAINING_DIFFICULTY_OPTIONS,
  QUESTION_STATUS_LABELS,
} from '@/types/training'
import type { TrainingDifficulty, TrainingQuestionStatus, MasteryLevel } from '@/types/training'

const trainingStore = useTrainingStore()

const filters = reactive({
  topic: '',
  masteryLevel: '' as MasteryLevel | '',
  includeMastered: false,
  scoreMax: undefined as number | undefined,
})

const masteryFilterOptions = MASTERY_LEVEL_OPTIONS.filter(o => o.value !== 'mastered')

const weakCount = computed(() => trainingStore.mistakes.filter(m => m.masteryLevel === 'weak').length)
const basicCount = computed(() => trainingStore.mistakes.filter(m => m.masteryLevel === 'basic').length)
const goodCount = computed(() => trainingStore.mistakes.filter(m => m.masteryLevel === 'good').length)

onMounted(() => {
  trainingStore.fetchMistakes()
})

function handleFilter() {
  const params: Record<string, unknown> = {}
  if (filters.topic) params.topic = filters.topic
  if (filters.masteryLevel) params.masteryLevel = filters.masteryLevel
  if (filters.includeMastered) params.includeMastered = true
  if (filters.scoreMax !== undefined) params.scoreMax = filters.scoreMax
  trainingStore.fetchMistakes(params as any)
}

function handleReset() {
  filters.topic = ''
  filters.masteryLevel = ''
  filters.includeMastered = false
  filters.scoreMax = undefined
  trainingStore.fetchMistakes()
}

async function handleMaster(id: number) {
  const ok = await trainingStore.masterQuestion(id)
  if (ok) {
    ElMessage.success('已标记为已掌握')
    handleFilter()
  } else {
    ElMessage.error('操作失败')
  }
}

function masteryColor(level: MasteryLevel) {
  return MASTERY_LEVEL_OPTIONS.find(o => o.value === level)?.color || '#999'
}

function masteryLabel(level: MasteryLevel) {
  return MASTERY_LEVEL_OPTIONS.find(o => o.value === level)?.label || level
}

function difficultyTagType(d: TrainingDifficulty) {
  if (d === 'easy') return 'success'
  if (d === 'medium') return 'warning'
  return 'danger'
}

function difficultyLabel(d: TrainingDifficulty) {
  return TRAINING_DIFFICULTY_OPTIONS.find(o => o.value === d)?.label ?? d
}

function statusTagType(s: TrainingQuestionStatus) {
  if (s === 'mastered') return 'success'
  if (s === 'reviewed') return ''
  if (s === 'answered') return 'warning'
  if (s === 'skipped') return 'info'
  return 'danger'
}
</script>

<style scoped>
.tm-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tm-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tm-page__title {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.tm-page__filter-card {
  padding: 16px 20px;
}

.tm-page__filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tm-page__filter-input {
  width: 200px;
}

.tm-page__filter-select {
  width: 160px;
}

.tm-page__switch-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tm-page__switch-label {
  font-size: 13px;
  color: var(--nb-muted);
  white-space: nowrap;
}

.tm-page__summary-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.tm-page__summary-card {
  text-align: center;
  padding: 20px 16px;
}

.tm-page__summary-value {
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 700;
  color: var(--nb-primary);
  margin-bottom: 4px;
}

.tm-page__summary-value--danger { color: #e74c3c; }
.tm-page__summary-value--warning { color: #f39c12; }
.tm-page__summary-value--success { color: #2ecc71; }

.tm-page__summary-label {
  font-size: 13px;
  color: var(--nb-muted);
}

.tm-page__mistake-item {
  display: flex;
  flex-direction: column;
}

.tm-page__mistake-header {
  margin-bottom: 16px;
}

.tm-page__mistake-title {
  font-family: var(--font-heading);
  font-size: 17px;
  font-weight: 600;
  color: var(--nb-text);
  text-decoration: none;
  display: block;
  margin-bottom: 10px;
}

.tm-page__mistake-title:hover {
  color: var(--nb-primary);
}

.tm-page__mistake-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tm-page__tag {
  border: var(--nb-border)  !important;
  box-shadow: 2px 2px 0 var(--nb-border);
}

.tm-page__skill-tag {
  border: var(--nb-border)  !important;
  box-shadow: 2px 2px 0 var(--nb-border);
}

.tm-page__section {
  margin-bottom: 16px;
}

.tm-page__section-heading {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.tm-page__section-heading--red { color: #e74c3c; }
.tm-page__section-heading--orange { color: #f39c12; }
.tm-page__section-heading--blue { color: #3498db; }

.tm-page__section-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tm-page__section-list li {
  padding-left: 16px;
  position: relative;
  line-height: 1.6;
  font-size: 14px;
}

.tm-page__section-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: var(--nb-muted);
}

.tm-page__rec-answer {
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  padding: 12px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
  color: var(--nb-muted);
  margin: 0;
}

.tm-page__mistake-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: var(--nb-border);
}

.tm-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.tm-page__empty {
  text-align: center;
  padding: 40px 0;
  color: var(--nb-muted);
  font-size: 15px;
}

@media (max-width: 768px) {
  .tm-page__summary-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .tm-page__filter-input,
  .tm-page__filter-select {
    width: 100%;
  }
}
</style>

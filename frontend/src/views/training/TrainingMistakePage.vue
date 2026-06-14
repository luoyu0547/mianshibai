<template>
  <MainLayout>
    <div class="tm-page">
      <NbPageHeader
        eyebrow="训练中心"
        title="错题本"
        description="集中查看 AI 批改标记的薄弱题目，针对性查漏补缺"
      />

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
          <NbButton variant="secondary" @click="handleReset">重置</NbButton>
        </div>
      </NbCard>

      <div class="tm-page__summary-row">
        <NbStatCard
          label="全部错题"
          :value="trainingStore.mistakes.length"
        />
        <NbStatCard
          label="薄弱"
          :value="weakCount"
          variant="danger"
        />
        <NbStatCard
          label="基础"
          :value="basicCount"
          variant="warning"
        />
        <NbStatCard
          label="良好"
          :value="goodCount"
          variant="success"
        />
      </div>

      <NbCard v-if="trainingStore.loading">
        <NbLoadingBlock title="加载错题..." :rows="4" />
      </NbCard>

      <template v-else-if="trainingStore.mistakes.length > 0">
        <NbCard
          v-for="mistake in trainingStore.mistakes"
          :key="mistake.questionId"
          :variant="mistake.masteryLevel === 'weak' ? 'danger' : mistake.masteryLevel === 'good' ? 'success' : 'warning'"
        >
          <div class="tm-page__mistake-header">
            <router-link
              :to="`/training/question/${mistake.questionId}`"
              class="tm-page__mistake-title"
            >
              {{ mistake.title }}
            </router-link>
            <div class="tm-page__mistake-tags">
              <NbStatusBadge :label="mistake.topic" variant="info" />
              <NbStatusBadge
                :label="getStatusDescriptor(trainingDifficultyMap, mistake.difficulty).label"
                :variant="getStatusDescriptor(trainingDifficultyMap, mistake.difficulty).variant"
              />
              <NbStatusBadge
                v-for="tag in mistake.skillTags"
                :key="tag"
                :label="tag"
                variant="default"
              />
              <NbStatusBadge
                :label="getStatusDescriptor(trainingMasteryMap, mistake.masteryLevel).label"
                :variant="getStatusDescriptor(trainingMasteryMap, mistake.masteryLevel).variant"
              />
              <span class="tm-page__score-chip">{{ mistake.latestScore }} 分</span>
              <NbStatusBadge
                :label="getStatusDescriptor(trainingQuestionStatusMap, mistake.status).label"
                :variant="getStatusDescriptor(trainingQuestionStatusMap, mistake.status).variant"
              />
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
            <p class="tm-page__rec-answer nb-prewrap">{{ mistake.recommendedAnswer.slice(0, 200) }}{{ mistake.recommendedAnswer.length > 200 ? '...' : '' }}</p>
          </div>

          <div class="tm-page__mistake-footer">
            <NbButton variant="success" @click="handleMaster(mistake.questionId)">标记已掌握</NbButton>
          </div>
        </NbCard>
      </template>

      <NbCard v-else>
        <NbEmptyState
          title="暂无错题"
          description="继续完成八股训练后这里会自动出现需要复习的题目"
        />
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useTrainingStore } from '@/stores/training'
import { MASTERY_LEVEL_OPTIONS } from '@/types/training'
import {
  trainingMasteryMap,
  trainingDifficultyMap,
  trainingQuestionStatusMap,
  getStatusDescriptor,
} from '@/utils/statusMaps'
import type { MasteryLevel, TrainingMistakeQueryRequest } from '@/types/training'

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
  trainingStore.fetchMistakes(params as TrainingMistakeQueryRequest)
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
</script>

<style scoped>
.tm-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
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

.tm-page__score-chip {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border: var(--nb-border);
  border-radius: var(--nb-radius-sm);
  background: rgba(108, 92, 231, 0.12);
  color: var(--nb-primary);
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

.tm-page__section-heading--red { color: var(--nb-danger); }
.tm-page__section-heading--orange { color: var(--nb-warning); }
.tm-page__section-heading--blue { color: var(--nb-secondary); }

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
  content: '\2022';
  position: absolute;
  left: 0;
  color: var(--nb-muted);
}

.tm-page__rec-answer {
  background: var(--nb-muted-surface);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  padding: 12px;
  line-height: 1.6;
  font-size: 14px;
  color: var(--nb-muted);
  margin: 0;
}

.tm-page__mistake-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: var(--nb-border);
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

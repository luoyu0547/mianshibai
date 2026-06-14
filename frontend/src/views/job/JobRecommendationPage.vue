<!-- src/views/job/JobRecommendationPage.vue -->
<template>
  <MainLayout>
    <div class="job-recommendation-page">
      <NbPageHeader
        eyebrow="职位情报"
        title="职位推荐"
        description="AI 根据职位库为你推荐"
      >
        <template #actions>
          <NbButton variant="ghost" @click="router.push('/job/import')">手动补充</NbButton>
        </template>
      </NbPageHeader>

      <div class="job-recommendation-page__toolbar">
        <div class="job-recommendation-page__resume-picker">
          <span class="job-recommendation-page__label">选择简历：</span>
          <el-select
            v-if="resumeStore.resumeList.length > 0"
            v-model="selectedResumeId"
            placeholder="请选择你的简历"
            size="large"
            style="width: 240px;"
            clearable
          >
            <el-option
              v-for="resume in resumeStore.resumeList"
              :key="resume.id"
              :label="resume.title"
              :value="resume.id"
            />
          </el-select>
          <span v-else class="job-recommendation-page__no-resume">
            请先创建简历
            <NbButton variant="ghost" @click="router.push('/resume/new')">去创建</NbButton>
          </span>
        </div>
        <NbButton
          variant="primary"
          :loading="jobStore.recommendationsLoading"
          :disabled="!selectedResumeId"
          @click="handleRefine"
        >
          精排推荐
        </NbButton>
      </div>

      <NbCard v-if="jobStore.recommendationsLoading">
        <NbLoadingBlock title="AI 分析推荐中..." :rows="4" />
      </NbCard>

      <NbCard v-else-if="!hasFetched && jobStore.recommendations.length === 0">
        <NbEmptyState
          title="暂无职位推荐"
          description="请先保存简历，然后选择简历进行精排推荐"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/resume/new')">创建简历</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <div v-else class="job-recommendation-page__grid">
        <NbCard
          v-for="rec in jobStore.recommendations"
          :key="rec.id"
          class="job-rec-card"
          :variant="cardVariant(rec.recommendation)"
        >
          <div class="job-rec-card__header">
            <h3 class="job-rec-card__title">{{ rec.job?.title || '未知名' }}</h3>
            <NbStatusBadge
              :label="getStatusDescriptor(jobRecommendationMap, rec.recommendation).label"
              :variant="getStatusDescriptor(jobRecommendationMap, rec.recommendation).variant"
            />
          </div>

          <div class="job-rec-card__meta">
            <span>{{ rec.job?.companyName }}</span>
            <span class="job-rec-card__divider">|</span>
            <span>{{ rec.job?.city }}</span>
            <span class="job-rec-card__divider">|</span>
            <span class="job-rec-card__salary">{{ rec.job?.salaryRange }}</span>
          </div>

          <div class="job-rec-card__score">
            <span>匹配度</span>
            <span class="job-rec-card__score-value">{{ scoreDisplay }}</span>
          </div>

          <div class="job-rec-card__reason" @click="toggleExpand(rec.id)">
            <p :class="{ 'job-rec-card__reason--collapsed': !expandedIds.has(rec.id) }">
              {{ rec.reason || '暂无分析理由' }}
            </p>
            <span v-if="needsExpand(rec.reason)" class="job-rec-card__expand-link">
              {{ expandedIds.has(rec.id) ? '收起' : '展开' }}
            </span>
          </div>

          <div v-if="rec.riskPoints && rec.riskPoints.length > 0" class="job-rec-card__risks">
            <span class="job-rec-card__section-label">风险点：</span>
            <span
              v-for="(risk, idx) in rec.riskPoints"
              :key="idx"
              class="job-rec-card__risk-tag"
            >{{ risk }}</span>
          </div>

          <div v-if="rec.actionSuggestions && rec.actionSuggestions.length > 0" class="job-rec-card__actions-list">
            <span class="job-rec-card__section-label">行动建议：</span>
            <ul>
              <li v-for="(suggestion, idx) in rec.actionSuggestions" :key="idx">{{ suggestion }}</li>
            </ul>
          </div>

          <div class="job-rec-card__footer">
            <NbButton variant="primary" @click="handleApply(rec.id)">
              加入投递计划
            </NbButton>
            <NbButton variant="ghost" @click="router.push(`/job/${rec.job?.id}`)">
              查看详情
            </NbButton>
            <button class="job-rec-card__dismiss-link" @click="handleDismiss(rec.id)">
              不感兴趣
            </button>
          </div>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useJobStore } from '@/stores/job'
import { useResumeStore } from '@/stores/resume'
import { getStatusDescriptor, jobRecommendationMap } from '@/utils/statusMaps'
import type { JobRecommendationVO } from '@/types/job'

const router = useRouter()
const jobStore = useJobStore()
const resumeStore = useResumeStore()

const selectedResumeId = ref<number | null>(null)
const expandedIds = ref(new Set<number>())
const hasFetched = ref(false)

onMounted(async () => {
  await resumeStore.fetchResumeList()
})

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

function needsExpand(text?: string): boolean {
  if (!text) return false
  return text.length > 80
}

function scoreDisplay(rec: JobRecommendationVO): string {
  if (rec.matchResult && rec.matchResult.totalScore !== undefined) {
    return `${rec.matchResult.totalScore} 分`
  }
  return `${rec.roughScore} 分`
}

function cardVariant(recommendation: string): 'default' | 'elevated' | 'accent' | 'success' | 'warning' | 'danger' | 'ai' | 'muted' | 'bordered' {
  const map: Record<string, string> = {
    recommended: 'success',
    cautious: 'warning',
    stretch: 'accent',
    not_recommended: 'muted',
  }
  return (map[recommendation] || 'default') as 'default'
}

async function handleRefine() {
  if (!selectedResumeId.value) return
  hasFetched.value = true
  await jobStore.refineRecommendations(selectedResumeId.value)
}

async function handleDismiss(id: number) {
  await jobStore.dismissRecommendation(id)
}

async function handleApply(id: number) {
  const result = await jobStore.applyRecommendation(id)
  if (result) {
    router.push('/applications')
  }
}
</script>

<style scoped>
.job-recommendation-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.job-recommendation-page__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  background: var(--nb-surface);
  border: var(--nb-border);
  border-radius: var(--nb-radius-lg);
  box-shadow: var(--nb-shadow-xs);
}

.job-recommendation-page__resume-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.job-recommendation-page__label {
  font-weight: 600;
  font-size: 14px;
  color: var(--nb-ink);
  white-space: nowrap;
}

.job-recommendation-page__no-resume {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--nb-muted);
}

.job-recommendation-page__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.job-rec-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.job-rec-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.job-rec-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
  margin-right: 8px;
}

.job-rec-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--nb-muted);
  flex-wrap: wrap;
}

.job-rec-card__divider {
  color: var(--nb-border);
}

.job-rec-card__salary {
  color: var(--nb-accent);
  font-weight: 600;
}

.job-rec-card__score {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--nb-muted);
}

.job-rec-card__score-value {
  font-weight: 700;
  font-size: 20px;
  color: var(--nb-primary);
  font-family: var(--font-heading);
}

.job-rec-card__reason {
  font-size: 13px;
  color: var(--nb-muted);
  line-height: 1.5;
  cursor: pointer;
  position: relative;
}

.job-rec-card__reason p {
  margin: 0;
}

.job-rec-card__reason--collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.job-rec-card__expand-link {
  color: var(--nb-primary);
  font-size: 12px;
  font-weight: 600;
}

.job-rec-card__section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--nb-ink);
  margin-right: 4px;
}

.job-rec-card__risks {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
}

.job-rec-card__risk-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: var(--nb-radius-sm);
  background: var(--nb-danger-light);
  color: var(--nb-danger);
  font-weight: 600;
}

.job-rec-card__actions-list {
  font-size: 13px;
  color: var(--nb-muted);
}

.job-rec-card__actions-list ul {
  margin: 4px 0 0;
  padding-left: 18px;
}

.job-rec-card__actions-list li {
  margin-bottom: 2px;
  line-height: 1.5;
}

.job-rec-card__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid var(--nb-border-color-light);
}

.job-rec-card__dismiss-link {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--nb-muted);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.job-rec-card__dismiss-link:hover {
  color: var(--nb-danger);
}
</style>

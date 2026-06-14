<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAnalyticsOverview, getReviewAnalytics } from '@/api/statistics'
import type { AnalyticsOverviewVO, ReviewAnalyticsVO } from '@/types/statistics'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import BaseChart from '@/components/charts/BaseChart.vue'
import { buildRadarOption, buildScoreTrendOption, buildSkillGapOption } from '@/utils/charts/reviewCharts'

const overview = ref<AnalyticsOverviewVO | null>(null)
const reviewAnalytics = ref<ReviewAnalyticsVO | null>(null)
const loading = ref(false)

async function loadOverview() {
  loading.value = true
  try {
    const res = await getAnalyticsOverview()
    overview.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadReviewAnalytics() {
  try {
    const res = await getReviewAnalytics()
    if (res.code === 0) {
      reviewAnalytics.value = res.data
    }
  } catch {
    // Review analytics is optional, don't block page load
  }
}

const analyticsRadarOption = computed(() => {
  if (!reviewAnalytics.value || Object.keys(reviewAnalytics.value.radar).length === 0) return null
  return buildRadarOption(reviewAnalytics.value.radar, '能力均值雷达')
})

const scoreTrendOption = computed(() => {
  if (!reviewAnalytics.value || reviewAnalytics.value.recentScoreTrend.length === 0) return null
  return buildScoreTrendOption(reviewAnalytics.value.recentScoreTrend)
})

const skillGapOption = computed(() => {
  if (!reviewAnalytics.value || reviewAnalytics.value.topSkillGaps.length === 0) return null
  return buildSkillGapOption(reviewAnalytics.value.topSkillGaps)
})

onMounted(() => {
  loadOverview()
  loadReviewAnalytics()
})
</script>

<template>
  <MainLayout>
    <div class="analytics-page">
      <NbPageHeader
        eyebrow="数据分析"
        title="求职分析看板"
        description="一站式掌握简历、职位、面试与能力维度的全景数据"
      />

      <NbCard v-if="loading">
        <NbLoadingBlock title="加载分析数据..." :rows="4" />
      </NbCard>

      <template v-else>
        <div v-if="overview" class="analytics-page__stats">
          <NbStatCard
            label="简历数"
            :value="overview.resumeCount"
          />
          <NbStatCard
            label="职位数"
            :value="overview.jobCount"
          />
          <NbStatCard
            label="面试数"
            :value="overview.interviewCount"
          />
          <NbStatCard
            label="平均面试分"
            :value="overview.averageInterviewScore"
            variant="primary"
          />
        </div>

        <div class="analytics-page__grid">
          <NbCard>
            <NbSectionTitle title="高频技能缺口" />
            <div v-if="overview && overview.topMissingSkills.length > 0" class="analytics-page__skill-tags">
              <span v-for="skill in overview.topMissingSkills" :key="skill" class="analytics-page__skill-chip">{{ skill }}</span>
            </div>
            <NbEmptyState
              v-else
              title="暂无数据"
              description="完成更多面试后将生成分析"
            />
          </NbCard>
          <NbCard>
            <NbSectionTitle title="下一步行动" />
            <div v-if="overview && overview.nextActions.length > 0" class="analytics-page__action-list">
              <div v-for="(action, i) in overview.nextActions" :key="i" class="analytics-page__action-item">
                <span class="analytics-page__action-num">{{ i + 1 }}</span>
                <span>{{ action }}</span>
              </div>
            </div>
            <NbEmptyState
              v-else
              title="暂无行动建议"
            />
          </NbCard>
        </div>

        <div v-if="reviewAnalytics" class="analytics-page__grid">
          <NbCard>
            <BaseChart v-if="analyticsRadarOption" :option="analyticsRadarOption" title="能力雷达" height="320px" />
            <NbEmptyState
              v-else
              title="能力雷达待生成"
              description="完成带增强复盘的面试后，这里会展示能力雷达"
              variant="ai"
            />
          </NbCard>
          <NbCard>
            <BaseChart v-if="skillGapOption" :option="skillGapOption" title="Top 技能缺口" height="320px" />
            <NbEmptyState
              v-else
              title="暂无技能缺口数据"
            />
          </NbCard>
        </div>

        <div
          v-if="reviewAnalytics && (reviewAnalytics.recentScoreTrend.length > 0 || reviewAnalytics.latestActionItems.length > 0)"
          class="analytics-page__grid"
        >
          <NbCard>
            <BaseChart v-if="scoreTrendOption" :option="scoreTrendOption" title="近期面试分数" height="320px" />
            <NbEmptyState
              v-else
              title="暂无近期分数趋势"
            />
          </NbCard>
          <NbCard>
            <NbSectionTitle title="最新行动建议" />
            <div v-if="reviewAnalytics.latestActionItems.length > 0" class="analytics-page__action-list">
              <div v-for="(item, i) in reviewAnalytics.latestActionItems" :key="i" class="analytics-page__action-item">
                <span class="analytics-page__action-num">{{ i + 1 }}</span>
                <span>{{ item }}</span>
              </div>
            </div>
          </NbCard>
        </div>
      </template>
    </div>
  </MainLayout>
</template>

<style scoped>
.analytics-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.analytics-page__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.analytics-page__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.analytics-page__skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.analytics-page__skill-chip {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius-sm);
  box-shadow: var(--nb-shadow-xs);
  background: var(--nb-muted-surface);
  color: var(--nb-ink);
}

.analytics-page__action-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-top: 16px;
}

.analytics-page__action-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(45, 52, 54, 0.1);
  font-size: 14px;
  line-height: 1.6;
}

.analytics-page__action-item:last-child {
  border-bottom: none;
}

.analytics-page__action-num {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: var(--nb-primary);
  border-radius: var(--nb-radius-sm);
}

@media (max-width: 768px) {
  .analytics-page__stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .analytics-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>

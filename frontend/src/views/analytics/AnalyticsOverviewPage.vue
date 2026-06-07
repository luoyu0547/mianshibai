<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAnalyticsOverview, getReviewAnalytics } from '@/api/statistics'
import type { AnalyticsOverviewVO, ReviewAnalyticsVO } from '@/types/statistics'
import NbCard from '@/components/NbCard.vue'

const overview = ref<AnalyticsOverviewVO | null>(null)
const reviewAnalytics = ref<ReviewAnalyticsVO | null>(null)
const loading = ref(false)

async function loadOverview() {
  loading.value = true
  try {
    const res = await getAnalyticsOverview()
    overview.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function loadReviewAnalytics() {
  try {
    const res = await getReviewAnalytics()
    if (res.data.code === 0) {
      reviewAnalytics.value = res.data.data
    }
  } catch {
    // Review analytics is optional, don't block page load
  }
}

function radarLabel(key: string) {
  const labels: Record<string, string> = {
    accuracy: '技术准确性',
    clarity: '表达清晰度',
    depth: '项目深度',
    matching: '岗位匹配度',
    systemDesign: '系统设计',
  }
  return labels[key] ?? key
}

onMounted(() => {
  loadOverview()
  loadReviewAnalytics()
})
</script>

<template>
  <div class="analytics-page">
    <h2>求职分析看板</h2>
    <div v-loading="loading">
      <div v-if="overview" style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px;">
        <NbCard>
          <div style="text-align: center;">
            <div style="font-size: 32px; font-weight: bold;">{{ overview.resumeCount }}</div>
            <div style="color: #999;">简历数</div>
          </div>
        </NbCard>
        <NbCard>
          <div style="text-align: center;">
            <div style="font-size: 32px; font-weight: bold;">{{ overview.jobCount }}</div>
            <div style="color: #999;">职位数</div>
          </div>
        </NbCard>
        <NbCard>
          <div style="text-align: center;">
            <div style="font-size: 32px; font-weight: bold;">{{ overview.interviewCount }}</div>
            <div style="color: #999;">面试数</div>
          </div>
        </NbCard>
        <NbCard>
          <div style="text-align: center;">
            <div style="font-size: 32px; font-weight: bold; color: #6C5CE7;">{{ overview.averageInterviewScore }}</div>
            <div style="color: #999;">平均面试分</div>
          </div>
        </NbCard>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <NbCard>
          <h3>高频技能缺口</h3>
          <div v-if="overview && overview.topMissingSkills.length > 0">
            <el-tag v-for="skill in overview.topMissingSkills" :key="skill" style="margin: 4px;">{{ skill }}</el-tag>
          </div>
          <div v-else style="color: #999; padding: 20px; text-align: center;">
            暂无数据，完成更多面试后将生成分析
          </div>
        </NbCard>
        <NbCard>
          <h3>下一步行动</h3>
          <div v-if="overview">
            <div v-for="(action, i) in overview.nextActions" :key="i" style="padding: 8px 0; border-bottom: 1px solid #eee;">
              {{ i + 1 }}. {{ action }}
            </div>
          </div>
        </NbCard>
      </div>

      <div v-if="reviewAnalytics" style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 24px;">
        <NbCard>
          <h3>能力雷达</h3>
          <div v-if="Object.keys(reviewAnalytics.radar).length > 0">
            <div v-for="(score, key) in reviewAnalytics.radar" :key="key" style="margin-bottom: 12px;">
              <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                <span style="font-size: 14px;">{{ radarLabel(key) }}</span>
                <span style="font-weight: 600;">{{ score }}</span>
              </div>
              <el-progress :percentage="score" :stroke-width="10" :show-text="false" color="#6C5CE7" />
            </div>
          </div>
          <div v-else style="color: #999; padding: 20px; text-align: center;">
            完成带增强复盘的面试后，这里会展示能力雷达
          </div>
        </NbCard>
        <NbCard>
          <h3>Top 技能缺口</h3>
          <div v-if="reviewAnalytics.topSkillGaps.length > 0">
            <el-tag v-for="gap in reviewAnalytics.topSkillGaps" :key="gap.name" type="warning" style="margin: 4px;">
              {{ gap.name }} · {{ gap.count }} 次
            </el-tag>
          </div>
          <div v-else style="color: #999; padding: 20px; text-align: center;">
            暂无技能缺口数据
          </div>
        </NbCard>
      </div>

      <div v-if="reviewAnalytics && (reviewAnalytics.recentScoreTrend.length > 0 || reviewAnalytics.latestActionItems.length > 0)" style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px;">
        <NbCard>
          <h3>近期面试分数</h3>
          <div v-if="reviewAnalytics.recentScoreTrend.length > 0">
            <div v-for="(item, i) in reviewAnalytics.recentScoreTrend" :key="i" style="display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; font-size: 14px;">
              <span>{{ item.date }}</span>
              <span style="font-weight: 600; color: #6C5CE7;">{{ item.score }} 分</span>
            </div>
          </div>
        </NbCard>
        <NbCard>
          <h3>最新行动建议</h3>
          <div v-if="reviewAnalytics.latestActionItems.length > 0">
            <div v-for="(item, i) in reviewAnalytics.latestActionItems" :key="i" style="padding: 8px 0; border-bottom: 1px solid #eee; font-size: 14px;">
              {{ i + 1 }}. {{ item }}
            </div>
          </div>
        </NbCard>
      </div>
    </div>
  </div>
</template>

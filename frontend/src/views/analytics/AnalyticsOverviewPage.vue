<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAnalyticsOverview } from '@/api/statistics'
import type { AnalyticsOverviewVO } from '@/types/statistics'
import NbCard from '@/components/NbCard.vue'

const overview = ref<AnalyticsOverviewVO | null>(null)
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

onMounted(() => {
  loadOverview()
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listJobs } from '@/api/job'
import type { JobVO, JobListQueryRequest } from '@/types/job'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { getStatusDescriptor, jobRecommendationMap } from '@/utils/statusMaps'

const router = useRouter()
const jobs = ref<JobVO[]>([])
const loading = ref(false)
const query = ref<JobListQueryRequest>({})

async function loadJobs() {
  loading.value = true
  try {
    const res = await listJobs(query.value)
    jobs.value = res.data
  } finally {
    loading.value = false
  }
}

function goToDetail(id: number) {
  router.push(`/job/${id}`)
}

onMounted(() => {
  loadJobs()
})
</script>

<template>
  <MainLayout>
    <div class="job-list-page">
      <NbPageHeader
        eyebrow="职位情报"
        title="我的职位"
        description="管理已导入的职位，查看 AI 分析与简历匹配结果"
      >
        <template #actions>
          <NbButton variant="primary" @click="router.push('/job/import')">+ 导入职位</NbButton>
        </template>
      </NbPageHeader>

      <div class="job-list-page__filter">
        <el-input
          v-model="query.keyword"
          placeholder="搜索职位名称"
          clearable
          class="job-list-page__filter-input"
          @keyup.enter="loadJobs"
          @clear="loadJobs"
        />
        <el-input
          v-model="query.city"
          placeholder="城市"
          clearable
          class="job-list-page__filter-city"
          @keyup.enter="loadJobs"
          @clear="loadJobs"
        />
        <NbButton variant="primary" @click="loadJobs">搜索</NbButton>
      </div>

      <NbCard v-if="loading">
        <NbLoadingBlock title="加载职位..." :rows="4" />
      </NbCard>

      <NbCard v-else-if="jobs.length === 0">
        <NbEmptyState
          title="暂无职位"
          description="导入一个目标职位，获取 AI 深度分析与简历匹配"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/job/import')">去导入</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <div v-else class="job-list-page__grid">
        <NbCard
          v-for="job in jobs"
          :key="job.id"
          clickable
          class="job-list-card"
          @click="goToDetail(job.id)"
        >
          <div class="job-list-card__header">
            <h3 class="job-list-card__title">{{ job.title }}</h3>
            <NbStatusBadge
              v-if="job.matchResult && job.matchResult.recommendation"
              :label="getStatusDescriptor(jobRecommendationMap, job.matchResult.recommendation).label"
              :variant="getStatusDescriptor(jobRecommendationMap, job.matchResult.recommendation).variant"
            />
          </div>
          <div class="job-list-card__meta">
            <span>{{ job.companyName }}</span>
            <span class="job-list-card__divider">|</span>
            <span>{{ job.city }}</span>
            <span class="job-list-card__divider">|</span>
            <span class="job-list-card__salary">{{ job.salaryRange }}</span>
          </div>
          <div v-if="job.matchResult" class="job-list-card__score">
            <span class="job-list-card__score-label">匹配分</span>
            <span class="job-list-card__score-value">{{ job.matchResult.totalScore }}</span>
          </div>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<style scoped>
.job-list-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.job-list-page__filter {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.job-list-page__filter-input {
  width: 220px;
}

.job-list-page__filter-city {
  width: 120px;
}

.job-list-page__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.job-list-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.job-list-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
  margin-right: 8px;
}

.job-list-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--nb-muted);
  flex-wrap: wrap;
}

.job-list-card__divider {
  color: var(--nb-border);
}

.job-list-card__salary {
  color: var(--nb-accent);
  font-weight: 600;
}

.job-list-card__score {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 12px;
}

.job-list-card__score-label {
  font-size: 13px;
  color: var(--nb-muted);
}

.job-list-card__score-value {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--nb-primary);
}
</style>

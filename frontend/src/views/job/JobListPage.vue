<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listJobs } from '@/api/job'
import type { JobVO, JobListQueryRequest } from '@/types/job'
import { useRouter } from 'vue-router'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'

const router = useRouter()
const jobs = ref<JobVO[]>([])
const loading = ref(false)
const query = ref<JobListQueryRequest>({})

async function loadJobs() {
  loading.value = true
  try {
    const res = await listJobs(query.value)
    jobs.value = res.data.data
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
  <div class="job-list-page">
    <h2>我的职位</h2>
    <div class="filters" style="display: flex; gap: 12px; margin-bottom: 16px;">
      <el-input v-model="query.keyword" placeholder="搜索职位名称" clearable style="width: 200px;" @clear="loadJobs" />
      <el-input v-model="query.city" placeholder="城市" clearable style="width: 120px;" @clear="loadJobs" />
      <NbButton @click="loadJobs">搜索</NbButton>
    </div>
    <div v-loading="loading">
      <div v-if="jobs.length === 0" style="text-align: center; padding: 40px; color: #999;">
        暂无职位，去导入一个目标职位吧
      </div>
      <NbCard
        v-for="job in jobs"
        :key="job.id"
        style="margin-bottom: 12px; cursor: pointer;"
        @click="goToDetail(job.id)"
      >
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h3 style="margin: 0;">{{ job.title }}</h3>
            <p style="margin: 4px 0 0; color: #666;">{{ job.companyName }} · {{ job.city }} · {{ job.salaryRange }}</p>
          </div>
          <div v-if="job.matchResult" style="text-align: center;">
            <div style="font-size: 24px; font-weight: bold; color: #6C5CE7;">{{ job.matchResult.totalScore }}</div>
            <div style="font-size: 12px; color: #999;">匹配分</div>
          </div>
        </div>
      </NbCard>
    </div>
  </div>
</template>

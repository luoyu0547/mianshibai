import { ref } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { listJobCrawlTasks, createJobCrawlTask, updateJobCrawlTask, enableJobCrawlTask, disableJobCrawlTask, runJobCrawlTask, getJobCrawlTask, listJobCrawlTaskRuns, listJobCrawlRunItems } from '@/api/jobCrawl'
import type { AdminJobCrawlTaskVO, AdminJobCrawlTaskCreateRequest, AdminJobCrawlTaskUpdateRequest, AdminJobCrawlRunVO, AdminJobCrawlItemVO } from '@/types/jobCrawl'

export const useJobCrawlStore = defineStore('jobCrawl', () => {
  const tasks = ref<AdminJobCrawlTaskVO[]>([])
  const loading = ref(false)

  async function fetchTasks() {
    loading.value = true
    try {
      const res = await listJobCrawlTasks()
      tasks.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function create(data: AdminJobCrawlTaskCreateRequest) {
    const res = await createJobCrawlTask(data)
    if (res.data) {
      ElMessage.success('采集任务创建成功')
      return res.data
    }
  }

  async function update(id: number, data: AdminJobCrawlTaskUpdateRequest) {
    const res = await updateJobCrawlTask(id, data)
    if (res.data) {
      ElMessage.success('更新成功')
      return res.data
    }
  }

  async function toggleEnable(id: number, enabled: boolean) {
    const fn = enabled ? enableJobCrawlTask : disableJobCrawlTask
    const res = await fn(id)
    if (res.data) {
      ElMessage.success(enabled ? '已启用' : '已禁用')
      await fetchTasks()
    }
  }

  async function run(id: number) {
    const res = await runJobCrawlTask(id)
    if (res.data) {
      ElMessage.success('采集任务已开始运行')
      return res.data
    }
  }

  return { tasks, loading, fetchTasks, create, update, toggleEnable, run }
})

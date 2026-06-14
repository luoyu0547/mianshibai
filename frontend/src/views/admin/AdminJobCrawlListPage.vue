<template>
  <AdminLayout>
    <section class="admin-page">
      <NbPageHeader
        eyebrow="Job Crawl"
        title="职位采集任务"
        description="管理职位批量采集任务"
      >
        <template #actions>
          <NbButton variant="primary" @click="router.push('/admin/job-crawl/new')">+ 新建任务</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="crawlStore.loading">
        <NbLoadingBlock title="加载采集任务列表..." :rows="6" />
      </NbCard>

      <NbCard v-else-if="crawlStore.tasks.length === 0">
        <NbEmptyState
          title="暂无采集任务"
          description="点击「新建任务」创建第一条职位采集任务"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/admin/job-crawl/new')">新建任务</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <NbCard v-else>
        <el-table :data="crawlStore.tasks" empty-text="暂无任务">
          <el-table-column prop="name" label="任务名称" min-width="160" />
          <el-table-column label="采集来源" width="140">
            <template #default="{ row }">
              <NbStatusBadge
                :label="getSourceTypeLabel(row.sourceType)"
                variant="info"
              />
            </template>
          </el-table-column>
          <el-table-column label="调度方式" width="100">
            <template #default="{ row }">
              <NbStatusBadge
                :label="getScheduleTypeLabel(row.scheduleType)"
                :variant="row.scheduleType === 'manual' ? 'muted' : 'primary'"
              />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <NbStatusBadge
                :label="row.status === 'enabled' ? '已启用' : '已禁用'"
                :variant="row.status === 'enabled' ? 'success' : 'warning'"
              />
            </template>
          </el-table-column>
          <el-table-column label="上次运行" min-width="160">
            <template #default="{ row }">
              <span v-if="row.lastRunAt" class="cell-text">{{ row.lastRunAt }}</span>
              <span v-else class="cell-text cell-text--muted">尚未运行</span>
            </template>
          </el-table-column>
          <el-table-column label="成功/失败" width="110" align="center">
            <template #default="{ row }">
              <span class="cell-stats">
                <span class="cell-stats__success">{{ row.successCount ?? 0 }}</span>
                <span class="cell-stats__divider">/</span>
                <span class="cell-stats__failed">{{ row.failedCount ?? 0 }}</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="340" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <NbButton @click="router.push(`/admin/job-crawl/${row.id}`)">详情</NbButton>
                <el-switch
                  :model-value="row.status === 'enabled'"
                  @change="(val: boolean) => crawlStore.toggleEnable(row.id, val)"
                  active-text="启用"
                  inactive-text="禁用"
                />
                <NbButton variant="accent" @click="handleRun(row.id)">运行</NbButton>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </NbCard>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useJobCrawlStore } from '@/stores/jobCrawl'
import { JOB_CRAWL_SOURCE_TYPE_OPTIONS, JOB_CRAWL_SCHEDULE_TYPE_OPTIONS } from '@/types/jobCrawl'
import type { JobCrawlSourceType, JobCrawlScheduleType } from '@/types/jobCrawl'

const router = useRouter()
const crawlStore = useJobCrawlStore()

function getSourceTypeLabel(type: JobCrawlSourceType): string {
  return JOB_CRAWL_SOURCE_TYPE_OPTIONS.find(o => o.value === type)?.label ?? type
}

function getScheduleTypeLabel(type: JobCrawlScheduleType): string {
  return JOB_CRAWL_SCHEDULE_TYPE_OPTIONS.find(o => o.value === type)?.label ?? type
}

async function handleRun(id: number) {
  const result = await crawlStore.run(id)
  if (result) {
    await crawlStore.fetchTasks()
  }
}

onMounted(() => {
  crawlStore.fetchTasks()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cell-text {
  font-size: 13px;
  color: var(--nb-ink);
}

.cell-text--muted {
  color: var(--nb-muted);
}

.cell-stats {
  font-size: 13px;
  font-weight: 600;
}

.cell-stats__success {
  color: var(--nb-success);
}

.cell-stats__divider {
  color: var(--nb-muted-light);
  margin: 0 2px;
}

.cell-stats__failed {
  color: var(--nb-danger);
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>

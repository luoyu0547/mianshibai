<template>
  <AdminLayout>
    <section class="admin-page">
      <NbPageHeader
        eyebrow="Job Crawl"
        :title="task?.name ?? '采集任务详情'"
        description="查看任务配置与运行历史"
      >
        <template #actions>
          <NbButton variant="accent" @click="handleRun" :loading="running">立即运行</NbButton>
          <NbButton variant="ghost" @click="router.push('/admin/job-crawl')">返回列表</NbButton>
        </template>
      </NbPageHeader>

      <NbLoadingBlock v-if="loading" title="加载任务详情..." :rows="8" />

      <template v-else-if="task">
        <NbCard>
          <NbSectionTitle title="任务配置" />
          <div class="config-grid">
            <div class="config-item">
              <span class="config-label">任务名称</span>
              <span class="config-value">{{ task.name }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">采集来源</span>
              <span class="config-value">{{ getSourceTypeLabel(task.sourceType) }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">调度方式</span>
              <NbStatusBadge
                :label="getScheduleTypeLabel(task.scheduleType)"
                :variant="task.scheduleType === 'manual' ? 'muted' : 'primary'"
              />
            </div>
            <div class="config-item">
              <span class="config-label">状态</span>
              <NbStatusBadge
                :label="task.status === 'enabled' ? '已启用' : '已禁用'"
                :variant="task.status === 'enabled' ? 'success' : 'warning'"
              />
            </div>
            <div v-if="task.scheduleType === 'cron' && task.cronExpression" class="config-item">
              <span class="config-label">Cron表达式</span>
              <span class="config-value config-value--code">{{ task.cronExpression }}</span>
            </div>
            <div v-if="task.sourceUrl" class="config-item config-item--full">
              <span class="config-label">源URL</span>
              <span class="config-value config-value--break">{{ task.sourceUrl }}</span>
            </div>
            <div v-if="task.keywords" class="config-item">
              <span class="config-label">关键词</span>
              <span class="config-value">{{ task.keywords }}</span>
            </div>
            <div v-if="task.cities" class="config-item">
              <span class="config-label">城市</span>
              <span class="config-value">{{ task.cities }}</span>
            </div>
            <div v-if="task.experienceLevels" class="config-item">
              <span class="config-label">经验级别</span>
              <span class="config-value">{{ task.experienceLevels }}</span>
            </div>
            <div v-if="task.lastRunAt" class="config-item">
              <span class="config-label">上次运行</span>
              <span class="config-value">{{ task.lastRunAt }}</span>
            </div>
            <div v-if="task.nextRunAt" class="config-item">
              <span class="config-label">下次运行</span>
              <span class="config-value">{{ task.nextRunAt }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">成功/失败</span>
              <span class="config-value">
                <span class="cell-stats__success">{{ task.successCount ?? 0 }}</span>
                <span class="cell-stats__divider">/</span>
                <span class="cell-stats__failed">{{ task.failedCount ?? 0 }}</span>
              </span>
            </div>
            <div v-if="task.remark" class="config-item config-item--full">
              <span class="config-label">备注</span>
              <span class="config-value">{{ task.remark }}</span>
            </div>
          </div>
        </NbCard>

        <NbCard>
          <NbSectionTitle title="运行历史" description="最近采集运行记录" />
          <NbLoadingBlock v-if="runsLoading" title="加载运行记录..." :rows="4" />
          <NbEmptyState
            v-else-if="runs.length === 0"
            title="暂无运行记录"
            description="点击「立即运行」触发一次采集"
          />
          <el-table v-else :data="runs" empty-text="暂无运行记录">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <NbStatusBadge
                  :label="row.status"
                  :variant="row.status === 'completed' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'"
                />
              </template>
            </el-table-column>
            <el-table-column prop="startedAt" label="开始时间" min-width="160" />
            <el-table-column prop="finishedAt" label="结束时间" min-width="160" />
            <el-table-column label="总计/成功/重复/失败" min-width="200">
              <template #default="{ row }">
                <span class="run-stats">
                  {{ row.totalCount }} / {{ row.successCount }} / {{ row.duplicateCount }} / {{ row.failedCount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="错误信息" min-width="160">
              <template #default="{ row }">
                <span v-if="row.errorMessage" class="cell-text cell-text--danger">{{ row.errorMessage }}</span>
                <span v-else class="cell-text cell-text--muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </NbCard>
      </template>

      <NbCard v-else>
        <NbEmptyState
          title="任务不存在"
          description="该采集任务可能已被删除"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/admin/job-crawl')">返回列表</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useJobCrawlStore } from '@/stores/jobCrawl'
import { getJobCrawlTask, listJobCrawlTaskRuns } from '@/api/jobCrawl'
import { JOB_CRAWL_SOURCE_TYPE_OPTIONS, JOB_CRAWL_SCHEDULE_TYPE_OPTIONS } from '@/types/jobCrawl'
import type { AdminJobCrawlTaskVO, AdminJobCrawlRunVO, JobCrawlSourceType, JobCrawlScheduleType } from '@/types/jobCrawl'

const router = useRouter()
const route = useRoute()
const crawlStore = useJobCrawlStore()

const taskId = Number(route.params.id)
const loading = ref(true)
const running = ref(false)
const runsLoading = ref(false)
const task = ref<AdminJobCrawlTaskVO | null>(null)
const runs = ref<AdminJobCrawlRunVO[]>([])

function getSourceTypeLabel(type: JobCrawlSourceType): string {
  return JOB_CRAWL_SOURCE_TYPE_OPTIONS.find(o => o.value === type)?.label ?? type
}

function getScheduleTypeLabel(type: JobCrawlScheduleType): string {
  return JOB_CRAWL_SCHEDULE_TYPE_OPTIONS.find(o => o.value === type)?.label ?? type
}

async function loadTask() {
  loading.value = true
  try {
    const res = await getJobCrawlTask(taskId)
    task.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadRuns() {
  runsLoading.value = true
  try {
    const res = await listJobCrawlTaskRuns(taskId)
    runs.value = res.data ?? []
  } finally {
    runsLoading.value = false
  }
}

async function handleRun() {
  running.value = true
  try {
    await crawlStore.run(taskId)
    await loadTask()
    await loadRuns()
  } finally {
    running.value = false
  }
}

onMounted(() => {
  loadTask()
  loadRuns()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 40px;
}

.config-item--full {
  grid-column: 1 / -1;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-label {
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font-heading);
  color: var(--nb-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.config-value {
  font-size: 15px;
  color: var(--nb-ink);
  font-weight: 500;
}

.config-value--code {
  font-family: var(--font-mono);
  font-size: 13px;
  background: var(--nb-muted-surface);
  padding: 2px 8px;
  border-radius: var(--nb-radius);
  display: inline-block;
}

.config-value--break {
  word-break: break-all;
}

.cell-text {
  font-size: 13px;
}

.cell-text--muted {
  color: var(--nb-muted);
}

.cell-text--danger {
  color: var(--nb-danger);
}

.cell-stats__success {
  color: var(--nb-success);
  font-weight: 600;
}

.cell-stats__divider {
  color: var(--nb-muted-light);
  margin: 0 2px;
}

.cell-stats__failed {
  color: var(--nb-danger);
  font-weight: 600;
}

.run-stats {
  font-size: 13px;
  font-weight: 600;
  color: var(--nb-ink);
}

@media (max-width: 768px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>

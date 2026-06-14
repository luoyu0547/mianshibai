<!-- src/views/application/ApplicationListPage.vue -->
<template>
  <MainLayout>
    <div class="app-list-page">
      <NbPageHeader
        eyebrow="求职管理"
        title="投递管理"
        description="跟踪每一次投递的进度与待办"
      >
        <template #actions>
          <NbButton @click="router.push('/applications/todos')">待办中心</NbButton>
          <NbButton variant="primary" @click="router.push('/applications/new')">+ 新建投递</NbButton>
        </template>
      </NbPageHeader>

      <div v-if="applicationStore.stats" class="app-list-page__stats">
        <NbStatCard
          v-for="item in statCards"
          :key="item.key"
          :label="item.label"
          :value="item.value"
          :variant="item.statVariant"
          class="app-list-page__stat-card"
          @click="handleStatClick(item.key)"
        />
      </div>

      <div class="app-list-page__filter">
        <el-input
          v-model="keyword"
          placeholder="搜索公司 / 职位"
          clearable
          style="width: 240px;"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-select
          v-model="statusFilter"
          placeholder="全部状态"
          clearable
          style="width: 160px;"
          @change="loadData"
        >
          <el-option
            v-for="opt in APPLICATION_STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <NbButton variant="primary" @click="loadData">搜索</NbButton>
      </div>

      <NbCard v-if="applicationStore.loading">
        <NbLoadingBlock title="加载投递记录..." :rows="4" />
      </NbCard>

      <NbCard v-else-if="applicationStore.applications.length === 0">
        <NbEmptyState
          title="还没有投递记录"
          description="新建一条投递记录，开始跟踪进度"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/applications/new')">新建投递</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <div v-else class="kanban-board">
        <div v-for="col in columns" :key="col.key" class="kanban-column">
          <div class="kanban-column__header">
            <span>{{ col.title }}</span>
            <span class="kanban-column__count">{{ appsForColumn(col.statuses).length }}</span>
          </div>
          <div
            v-for="app in appsForColumn(col.statuses)"
            :key="app.id"
            class="kanban-card"
            @click="router.push('/applications/' + app.id)"
          >
            <h3 class="kanban-card__title">{{ app.jobTitle }}</h3>
            <div class="kanban-card__company">{{ app.companyName }}</div>
            <div class="kanban-card__meta">
              <span v-if="app.location">{{ app.location }}</span>
              <span v-if="app.salaryRange" style="color: var(--nb-accent)">{{ app.salaryRange }}</span>
              <span v-if="app.source" style="color: var(--nb-muted)">{{ app.source }}</span>
            </div>
            <div v-if="app.nextEventAt" class="kanban-card__event">
              下一事件: {{ formatDate(app.nextEventAt) }}
            </div>
            <div v-if="app.unfinishedTodoCount > 0" class="kanban-card__todo">
              {{ app.unfinishedTodoCount }} 个待办
            </div>
            <div v-if="nextStatusMap[app.status]" class="kanban-card__action" @click.stop>
              <el-button size="small" type="primary" @click="handleAdvance(app.id, app.status)">
                推进 → {{ getStatusDescriptor(applicationStatusMap, nextStatusMap[app.status]!).label }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useApplicationStore } from '@/stores/application'
import { APPLICATION_STATUS_OPTIONS } from '@/types/application'
import type { ApplicationStatus } from '@/types/application'
import { applicationStatusMap, getStatusDescriptor } from '@/utils/statusMaps'
import { formatDate } from '@/utils/date'

const router = useRouter()
const applicationStore = useApplicationStore()

const keyword = ref('')
const statusFilter = ref<ApplicationStatus | ''>('')

const nextStatusMap: Partial<Record<ApplicationStatus, ApplicationStatus>> = {
  pending_submit: 'submitted',
  submitted: 'hr_contact',
  hr_contact: 'written_test',
  written_test: 'first_interview',
  first_interview: 'second_interview',
  second_interview: 'final_interview',
  final_interview: 'offer',
}

const columns = [
  { key: 'pending_submit', title: '待投递', statuses: ['pending_submit'] as ReadonlyArray<ApplicationStatus> },
  { key: 'submitted', title: '已投递', statuses: ['submitted'] as ReadonlyArray<ApplicationStatus> },
  { key: 'contacting', title: '沟通中', statuses: ['hr_contact'] as ReadonlyArray<ApplicationStatus> },
  { key: 'interviewing', title: '笔试/面试', statuses: ['written_test', 'first_interview', 'second_interview', 'final_interview'] as ReadonlyArray<ApplicationStatus> },
  { key: 'offer', title: 'Offer', statuses: ['offer'] as ReadonlyArray<ApplicationStatus> },
  { key: 'closed', title: '已关闭', statuses: ['rejected', 'withdrawn'] as ReadonlyArray<ApplicationStatus> },
]

function appsForColumn(statuses: ReadonlyArray<ApplicationStatus>) {
  return applicationStore.applications.filter(a => statuses.includes(a.status))
}

const statCards = computed(() => {
  const s = applicationStore.stats!
  return [
    { key: 'total', label: '全部', value: s.total, statVariant: 'default' as const },
    { key: 'pending_submit', label: '待投递', value: s.pendingSubmit, statVariant: 'warning' as const },
    { key: 'submitted', label: '已投递', value: s.submitted, statVariant: 'accent' as const },
    { key: 'interviewing', label: '面试中', value: s.interviewing, statVariant: 'primary' as const },
    { key: 'offer', label: 'Offer', value: s.offer, statVariant: 'success' as const },
    { key: 'closed', label: '失败/放弃', value: s.closed, statVariant: 'danger' as const },
  ]
})

onMounted(() => {
  loadData()
})

async function loadData() {
  await Promise.all([
    applicationStore.fetchStats(),
    applicationStore.fetchApplications({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
    }),
  ])
}

function handleStatClick(key: string) {
  if (key === 'total') {
    statusFilter.value = ''
  } else {
    statusFilter.value = key as ApplicationStatus
  }
  loadData()
}

async function handleAdvance(id: number, currentStatus: ApplicationStatus) {
  const nextStatus = nextStatusMap[currentStatus]
  if (!nextStatus) return
  const result = await applicationStore.updateApplicationStatus(id, nextStatus)
  if (result) {
    ElMessage.success('已推进')
    loadData()
  } else {
    ElMessage.error('推进失败')
  }
}
</script>

<style scoped>
.app-list-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.app-list-page__stats {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.app-list-page__stat-card {
  cursor: pointer;
}

.app-list-page__filter {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.kanban-board {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 16px;
}

.kanban-column {
  flex: 0 0 280px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kanban-column__header {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  padding: 8px 0;
  border-bottom: 2px solid var(--nb-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kanban-column__count {
  background: var(--nb-bg);
  border: 2px solid var(--nb-border);
  border-radius: 12px;
  padding: 0 8px;
  font-size: 13px;
}

.kanban-card {
  background: var(--nb-card);
  border: 2px solid var(--nb-border);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  box-shadow: var(--nb-shadow-sm);
  transition: box-shadow 0.15s;
}

.kanban-card:hover {
  box-shadow: var(--nb-shadow);
}

.kanban-card__title {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 4px;
}

.kanban-card__company {
  font-size: 13px;
  color: var(--nb-muted);
}

.kanban-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--nb-muted);
}

.kanban-card__event {
  font-size: 12px;
  color: var(--nb-primary);
  margin-top: 4px;
}

.kanban-card__todo {
  font-size: 12px;
  color: var(--nb-warning);
  margin-top: 2px;
}

.kanban-card__action {
  margin-top: 8px;
  text-align: right;
}

@media (max-width: 768px) {
  .kanban-board {
    flex-direction: column;
    overflow-x: visible;
  }
  .kanban-column {
    flex: 1 1 auto;
  }
}

@media (max-width: 900px) {
  .app-list-page__stats {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>

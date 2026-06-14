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

      <div v-else class="app-list-page__grid">
        <NbCard
          v-for="app in applicationStore.applications"
          :key="app.id"
          hoverable
          class="app-card"
          @click="router.push(`/applications/${app.id}`)"
        >
          <div class="app-card__header">
            <h3 class="app-card__title">{{ app.jobTitle }}</h3>
            <NbStatusBadge
              :label="getStatusDescriptor(applicationStatusMap, app.status).label"
              :variant="getStatusDescriptor(applicationStatusMap, app.status).variant"
            />
          </div>
          <div class="app-card__meta">
            <span>{{ app.companyName }}</span>
            <span class="app-card__divider">|</span>
            <span>{{ app.location }}</span>
            <template v-if="app.salaryRange">
              <span class="app-card__divider">|</span>
              <span class="app-card__salary">{{ app.salaryRange }}</span>
            </template>
          </div>
          <div v-if="app.nextEventAt" class="app-card__event">
            下一事件: {{ formatDate(app.nextEventAt) }}
          </div>
          <div v-if="app.unfinishedTodoCount > 0" class="app-card__todo">
            {{ app.unfinishedTodoCount }} 个待办
          </div>
          <div class="app-card__actions" @click.stop>
            <el-select
              :model-value="app.status"
              size="small"
              style="width: 120px;"
              @change="(val: ApplicationStatus) => handleStatusChange(app.id, val)"
            >
              <el-option
                v-for="opt in APPLICATION_STATUS_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
        </NbCard>
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
import NbStatusBadge from '@/components/NbStatusBadge.vue'
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

async function handleStatusChange(id: number, status: ApplicationStatus) {
  const result = await applicationStore.updateApplicationStatus(id, status)
  if (result) {
    ElMessage.success('状态已更新')
    loadData()
  } else {
    ElMessage.error('状态更新失败')
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

.app-list-page__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.app-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.app-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.app-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
  margin-right: 8px;
}

.app-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--nb-muted);
  flex-wrap: wrap;
}

.app-card__divider {
  color: var(--nb-border);
}

.app-card__salary {
  color: var(--nb-accent);
  font-weight: 600;
}

.app-card__event {
  font-size: 13px;
  color: var(--nb-primary);
  font-weight: 500;
}

.app-card__todo {
  font-size: 13px;
  color: var(--nb-warning);
  font-weight: 500;
}

.app-card__actions {
  margin-top: 4px;
}

@media (max-width: 900px) {
  .app-list-page__stats {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>

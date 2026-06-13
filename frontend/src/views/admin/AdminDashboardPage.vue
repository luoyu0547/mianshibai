<template>
  <AdminLayout>
    <section class="admin-page">
      <div class="admin-page__header">
        <p class="admin-page__eyebrow">Admin Overview</p>
        <h1>平台总览</h1>
        <p>查看用户、简历、面试、投递和训练数据的核心运营指标。</p>
      </div>

      <el-skeleton v-if="adminStore.loading && !adminStore.overview" :rows="6" animated />
      <div v-else class="metric-grid">
        <NbCard v-for="metric in metrics" :key="metric.label" class="metric-card">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </NbCard>
      </div>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useAdminStore } from '@/stores/admin'

const adminStore = useAdminStore()

const metrics = computed(() => {
  const overview = adminStore.overview
  return [
    { label: '用户总数', value: overview?.totalUsers ?? 0 },
    { label: '正常用户', value: overview?.enabledUsers ?? 0 },
    { label: '禁用用户', value: overview?.disabledUsers ?? 0 },
    { label: '管理员', value: overview?.adminUsers ?? 0 },
    { label: '简历数量', value: overview?.resumeCount ?? 0 },
    { label: '面试数量', value: overview?.interviewCount ?? 0 },
    { label: '已完成面试', value: overview?.completedInterviewCount ?? 0 },
    { label: '投递记录', value: overview?.applicationCount ?? 0 },
    { label: '训练计划', value: overview?.trainingPlanCount ?? 0 },
    { label: '八股作答', value: overview?.trainingAnswerCount ?? 0 },
    { label: 'AI 批改', value: overview?.trainingReviewCount ?? 0 },
  ]
})

onMounted(() => {
  adminStore.fetchOverview()
})
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 24px;
}

.admin-page__header h1 {
  margin: 0;
  font-size: 36px;
}

.admin-page__header p {
  margin: 8px 0 0;
  color: var(--nb-muted);
}

.admin-page__eyebrow {
  margin: 0 0 6px !important;
  color: var(--nb-primary) !important;
  font-weight: 800;
  text-transform: uppercase;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 18px;
}

.metric-card {
  display: grid;
  gap: 10px;
}

.metric-card span {
  color: var(--nb-muted);
  font-weight: 700;
}

.metric-card strong {
  font-size: 34px;
  line-height: 1;
}
</style>

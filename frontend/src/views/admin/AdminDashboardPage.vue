<template>
  <AdminLayout>
    <section class="admin-page">
      <NbPageHeader
        eyebrow="Admin Overview"
        title="平台总览"
        description="查看用户、简历、面试、投递和训练数据的核心运营指标。"
      />

      <NbCard v-if="adminStore.loading && !adminStore.overview">
        <NbLoadingBlock title="加载运营数据..." :rows="6" />
      </NbCard>

      <div v-else class="metric-grid">
        <NbStatCard
          v-for="metric in metrics"
          :key="metric.key"
          :label="metric.label"
          :value="metric.value"
          :variant="metric.variant"
        />
      </div>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import { useAdminStore } from '@/stores/admin'

type StatVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'accent'

const adminStore = useAdminStore()

const metrics = computed(() => {
  const overview = adminStore.overview
  return [
    { key: 'totalUsers', label: '用户总数', value: overview?.totalUsers ?? 0, variant: 'primary' as StatVariant },
    { key: 'enabledUsers', label: '正常用户', value: overview?.enabledUsers ?? 0, variant: 'success' as StatVariant },
    { key: 'disabledUsers', label: '禁用用户', value: overview?.disabledUsers ?? 0, variant: 'danger' as StatVariant },
    { key: 'adminUsers', label: '管理员', value: overview?.adminUsers ?? 0, variant: 'accent' as StatVariant },
    { key: 'resumeCount', label: '简历数量', value: overview?.resumeCount ?? 0, variant: 'default' as StatVariant },
    { key: 'interviewCount', label: '面试数量', value: overview?.interviewCount ?? 0, variant: 'default' as StatVariant },
    { key: 'completedInterviewCount', label: '已完成面试', value: overview?.completedInterviewCount ?? 0, variant: 'success' as StatVariant },
    { key: 'applicationCount', label: '投递记录', value: overview?.applicationCount ?? 0, variant: 'default' as StatVariant },
    { key: 'trainingPlanCount', label: '训练计划', value: overview?.trainingPlanCount ?? 0, variant: 'default' as StatVariant },
    { key: 'trainingAnswerCount', label: '八股作答', value: overview?.trainingAnswerCount ?? 0, variant: 'default' as StatVariant },
    { key: 'trainingReviewCount', label: 'AI 批改', value: overview?.trainingReviewCount ?? 0, variant: 'warning' as StatVariant },
  ]
})

onMounted(() => {
  adminStore.fetchOverview()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 18px;
}
</style>

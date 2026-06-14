<!-- src/views/job/JobFavoritePage.vue -->
<template>
  <MainLayout>
    <div class="job-favorite-page">
      <NbPageHeader
        eyebrow="职位情报"
        title="收藏的职位"
        description="关注的目标职位与 AI 匹配建议"
      >
        <template #actions>
          <NbButton variant="primary" @click="router.push('/job/import')">+ 导入职位</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="jobStore.loading">
        <NbLoadingBlock title="加载收藏..." :rows="4" />
      </NbCard>

      <NbCard v-else-if="jobStore.favoriteList.length === 0">
        <NbEmptyState
          title="还没有收藏的职位"
          description="导入并收藏感兴趣的职位，获取 AI 分析与匹配"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/job/import')">去导入</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <div v-else class="job-favorite-page__grid">
        <NbCard
          v-for="fav in jobStore.favoriteList"
          :key="fav.id"
          hoverable
          class="job-fav-card"
          @click="router.push(`/job/${fav.id}`)"
        >
          <div class="job-fav-card__header">
            <h3 class="job-fav-card__title">{{ fav.title }}</h3>
            <NbStatusBadge
              v-if="fav.matchResult && fav.matchResult.recommendation"
              :label="getStatusDescriptor(jobRecommendationMap, fav.matchResult.recommendation).label"
              :variant="getStatusDescriptor(jobRecommendationMap, fav.matchResult.recommendation).variant"
            />
          </div>
          <div class="job-fav-card__meta">
            <span>{{ fav.companyName }}</span>
            <span class="job-fav-card__divider">|</span>
            <span>{{ fav.city }}</span>
            <span class="job-fav-card__divider">|</span>
            <span class="job-fav-card__salary">{{ fav.salaryRange }}</span>
          </div>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useJobStore } from '@/stores/job'
import { getStatusDescriptor, jobRecommendationMap } from '@/utils/statusMaps'

const router = useRouter()
const jobStore = useJobStore()

onMounted(() => {
  jobStore.fetchFavoriteList()
})
</script>

<style scoped>
.job-favorite-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.job-favorite-page__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.job-fav-card {
  cursor: pointer;
}

.job-fav-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.job-fav-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
  margin-right: 8px;
}

.job-fav-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--nb-muted);
  flex-wrap: wrap;
}

.job-fav-card__divider {
  color: var(--nb-border);
}

.job-fav-card__salary {
  color: var(--nb-accent);
  font-weight: 600;
}
</style>

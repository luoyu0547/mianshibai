<!-- src/views/job/JobFavoritePage.vue -->
<template>
  <MainLayout>
    <div class="job-favorite-page">
      <div class="job-favorite-page__header">
        <h1 class="job-favorite-page__title">收藏的职位</h1>
        <NbButton type="primary" @click="router.push('/job/import')">+ 导入职位</NbButton>
      </div>

      <div v-if="jobStore.loading" class="job-favorite-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="jobStore.favoriteList.length === 0" class="job-favorite-page__empty">
        <div class="job-favorite-page__empty-icon">📋</div>
        <p class="job-favorite-page__empty-text">还没有收藏的职位</p>
        <NbButton type="primary" @click="router.push('/job/import')">去导入</NbButton>
      </div>

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
            <el-tag
              v-if="fav.matchResult && fav.matchResult.recommendation"
              :type="recTagType(fav.matchResult.recommendation)"
              size="small"
              class="job-fav-card__badge"
            >
              {{ fav.matchResult.recommendation }}
            </el-tag>
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
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useJobStore } from '@/stores/job'

const router = useRouter()
const jobStore = useJobStore()

onMounted(() => {
  jobStore.fetchFavoriteList()
})

function recTagType(rec: string) {
  if (rec.includes('强烈推荐') || rec.includes('高度匹配')) return 'success'
  if (rec.includes('推荐') || rec.includes('匹配')) return ''
  if (rec.includes('谨慎') || rec.includes('一般')) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.job-favorite-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.job-favorite-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.job-favorite-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.job-favorite-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.job-favorite-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 0;
}

.job-favorite-page__empty-icon {
  font-size: 64px;
}

.job-favorite-page__empty-text {
  font-size: 16px;
  color: var(--nb-muted);
  margin: 0;
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

.job-fav-card__badge {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
  flex-shrink: 0;
}

.job-fav-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--nb-muted);
}

.job-fav-card__divider {
  color: var(--nb-border);
}

.job-fav-card__salary {
  color: var(--nb-accent);
  font-weight: 600;
}
</style>

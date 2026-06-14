<template>
  <MainLayout>
    <div class="tma-page">
      <NbPageHeader
        eyebrow="训练中心"
        title="知识点掌握度"
        description="按知识点与技能标签维度追踪你的八股训练掌握情况"
      />

      <NbCard v-if="trainingStore.loading">
        <NbLoadingBlock title="加载掌握度数据..." :rows="4" />
      </NbCard>

      <template v-else>
        <div v-if="trainingStore.masterySummary" class="tma-page__stats">
          <NbStatCard
            label="薄弱"
            :value="trainingStore.masterySummary.weak"
            variant="danger"
          />
          <NbStatCard
            label="基础"
            :value="trainingStore.masterySummary.basic"
            variant="warning"
          />
          <NbStatCard
            label="良好"
            :value="trainingStore.masterySummary.good"
            variant="success"
          />
          <NbStatCard
            label="掌握"
            :value="trainingStore.masterySummary.mastered"
            variant="primary"
          />
        </div>

        <NbCard v-if="trainingStore.topicMasteries.length > 0">
          <NbSectionTitle title="Topic 掌握度" />
          <div class="tma-page__table">
            <div class="tma-page__table-head">
              <span class="tma-page__table-cell tma-page__cell-name">知识点</span>
              <span class="tma-page__table-cell tma-page__cell-level">掌握程度</span>
              <span class="tma-page__table-cell tma-page__cell-score">均分</span>
              <span class="tma-page__table-cell tma-page__cell-count">练习次数</span>
              <span class="tma-page__table-cell tma-page__cell-weak">薄弱</span>
              <span class="tma-page__table-cell tma-page__cell-mastered">已掌握</span>
              <span class="tma-page__table-cell tma-page__cell-time">最近练习</span>
            </div>
            <div
              v-for="item in trainingStore.topicMasteries"
              :key="item.id"
              class="tma-page__table-row"
              @click="router.push({ path: '/training/mistakes', query: { topic: item.targetName } })"
            >
              <span class="tma-page__table-cell tma-page__cell-name tma-page__cell-link">{{ item.targetName }}</span>
              <span class="tma-page__table-cell tma-page__cell-level">
                <NbStatusBadge
                  :label="getStatusDescriptor(trainingMasteryMap, item.masteryLevel).label"
                  :variant="getStatusDescriptor(trainingMasteryMap, item.masteryLevel).variant"
                />
              </span>
              <span class="tma-page__table-cell tma-page__cell-score">{{ item.averageScore.toFixed(1) }}</span>
              <span class="tma-page__table-cell tma-page__cell-count">{{ item.practiceCount }}</span>
              <span class="tma-page__table-cell tma-page__cell-weak">{{ item.weakCount }}</span>
              <span class="tma-page__table-cell tma-page__cell-mastered">{{ item.masteredCount }}</span>
              <span class="tma-page__table-cell tma-page__cell-time">{{ item.lastPracticedAt ? formatDateTime(item.lastPracticedAt) : '-' }}</span>
            </div>
          </div>
        </NbCard>

        <NbCard v-if="trainingStore.skillTagMasteries.length > 0">
          <NbSectionTitle title="技能标签掌握度" />
          <div class="tma-page__table">
            <div class="tma-page__table-head">
              <span class="tma-page__table-cell tma-page__cell-name">标签</span>
              <span class="tma-page__table-cell tma-page__cell-level">掌握程度</span>
              <span class="tma-page__table-cell tma-page__cell-weak">薄弱</span>
            </div>
            <div
              v-for="item in trainingStore.skillTagMasteries"
              :key="item.id"
              class="tma-page__table-row"
            >
              <span class="tma-page__table-cell tma-page__cell-name">{{ item.targetName }}</span>
              <span class="tma-page__table-cell tma-page__cell-level">
                <NbStatusBadge
                  :label="getStatusDescriptor(trainingMasteryMap, item.masteryLevel).label"
                  :variant="getStatusDescriptor(trainingMasteryMap, item.masteryLevel).variant"
                />
              </span>
              <span class="tma-page__table-cell tma-page__cell-weak">{{ item.weakCount }}</span>
            </div>
          </div>
        </NbCard>

        <div
          v-if="trainingStore.topicMasteries.length > 0 || trainingStore.skillTagMasteries.length > 0"
          class="tma-page__rebuild-row"
        >
          <NbButton variant="secondary" :loading="rebuilding" @click="handleRebuild">重建统计</NbButton>
        </div>

        <NbCard
          v-if="trainingStore.topicMasteries.length === 0 && trainingStore.skillTagMasteries.length === 0"
        >
          <NbEmptyState
            title="暂无掌握度数据"
            description="完成八股训练批改后这里会显示各知识点的掌握情况"
          />
        </NbCard>
      </template>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useTrainingStore } from '@/stores/training'
import { trainingMasteryMap, getStatusDescriptor } from '@/utils/statusMaps'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const trainingStore = useTrainingStore()
const rebuilding = ref(false)

onMounted(() => {
  trainingStore.fetchTopicMastery()
  trainingStore.fetchSkillTagMastery()
  trainingStore.fetchMasterySummary()
})

async function handleRebuild() {
  rebuilding.value = true
  try {
    const ok = await trainingStore.rebuildMastery()
    if (ok) {
      ElMessage.success('统计已重建')
      trainingStore.fetchTopicMastery()
      trainingStore.fetchSkillTagMastery()
      trainingStore.fetchMasterySummary()
    } else {
      ElMessage.error('重建失败')
    }
  } finally {
    rebuilding.value = false
  }
}
</script>

<style scoped>
.tma-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tma-page__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.tma-page__table {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-top: 16px;
}

.tma-page__table-head {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: var(--nb-muted-surface);
  border: var(--nb-border);
  border-radius: var(--nb-radius) var(--nb-radius) 0 0;
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  color: var(--nb-muted);
}

.tma-page__table-row {
  display: flex;
  align-items: center;
  padding: 12px;
  border: var(--nb-border);
  border-top: none;
  transition: var(--nb-transition);
  cursor: pointer;
}

.tma-page__table-row:last-child {
  border-radius: 0 0 var(--nb-radius) var(--nb-radius);
}

.tma-page__table-row:hover {
  background: rgba(108, 92, 231, 0.04);
}

.tma-page__table-cell {
  flex-shrink: 0;
  font-size: 14px;
}

.tma-page__cell-name { flex: 2; min-width: 120px; }
.tma-page__cell-level { flex: 1; min-width: 80px; text-align: center; }
.tma-page__cell-score { flex: 0 0 70px; text-align: center; font-weight: 600; }
.tma-page__cell-count { flex: 0 0 80px; text-align: center; }
.tma-page__cell-weak { flex: 0 0 60px; text-align: center; color: var(--nb-danger); font-weight: 600; }
.tma-page__cell-mastered { flex: 0 0 70px; text-align: center; color: var(--nb-primary); font-weight: 600; }
.tma-page__cell-time { flex: 1; min-width: 140px; text-align: right; font-size: 13px; color: var(--nb-muted); }

.tma-page__cell-link {
  color: var(--nb-primary);
  font-weight: 500;
}

.tma-page__rebuild-row {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

@media (max-width: 768px) {
  .tma-page__stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .tma-page__cell-time {
    display: none;
  }

  .tma-page__cell-mastered {
    display: none;
  }
}
</style>

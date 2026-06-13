<template>
  <MainLayout>
    <div class="tma-page">
      <div class="tma-page__header">
        <h2 class="tma-page__title">知识点掌握度</h2>
      </div>

      <div v-if="trainingStore.masterySummary" class="tma-page__summary-row">
        <NbCard class="tma-page__summary-card">
          <div class="tma-page__summary-value tma-page__summary-value--danger">{{ trainingStore.masterySummary.weak }}</div>
          <div class="tma-page__summary-label">薄弱</div>
        </NbCard>
        <NbCard class="tma-page__summary-card">
          <div class="tma-page__summary-value tma-page__summary-value--warning">{{ trainingStore.masterySummary.basic }}</div>
          <div class="tma-page__summary-label">基础</div>
        </NbCard>
        <NbCard class="tma-page__summary-card">
          <div class="tma-page__summary-value tma-page__summary-value--success">{{ trainingStore.masterySummary.good }}</div>
          <div class="tma-page__summary-label">良好</div>
        </NbCard>
        <NbCard class="tma-page__summary-card">
          <div class="tma-page__summary-value tma-page__summary-value--purple">{{ trainingStore.masterySummary.mastered }}</div>
          <div class="tma-page__summary-label">掌握</div>
        </NbCard>
      </div>

      <NbCard v-if="trainingStore.topicMasteries.length > 0">
        <h3 class="tma-page__section-title">Topic 掌握度</h3>
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
              <el-tag
                effect="dark"
                :color="masteryColor(item.masteryLevel)"
                size="small"
                style="border: none; color: #fff;"
              >
                {{ masteryLabel(item.masteryLevel) }}
              </el-tag>
            </span>
            <span class="tma-page__table-cell tma-page__cell-score">{{ item.averageScore.toFixed(1) }}</span>
            <span class="tma-page__table-cell tma-page__cell-count">{{ item.practiceCount }}</span>
            <span class="tma-page__table-cell tma-page__cell-weak">{{ item.weakCount }}</span>
            <span class="tma-page__table-cell tma-page__cell-mastered">{{ item.masteredCount }}</span>
            <span class="tma-page__table-cell tma-page__cell-time">{{ item.lastPracticedAt ? formatTime(item.lastPracticedAt) : '-' }}</span>
          </div>
        </div>
      </NbCard>

      <NbCard v-if="trainingStore.skillTagMasteries.length > 0">
        <h3 class="tma-page__section-title">技能标签掌握度</h3>
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
              <el-tag
                effect="dark"
                :color="masteryColor(item.masteryLevel)"
                size="small"
                style="border: none; color: #fff;"
              >
                {{ masteryLabel(item.masteryLevel) }}
              </el-tag>
            </span>
            <span class="tma-page__table-cell tma-page__cell-weak">{{ item.weakCount }}</span>
          </div>
        </div>
      </NbCard>

      <template v-if="trainingStore.topicMasteries.length > 0 || trainingStore.skillTagMasteries.length > 0">
        <div class="tma-page__rebuild-row">
          <NbButton type="secondary" :loading="rebuilding" @click="handleRebuild">重建统计</NbButton>
        </div>
      </template>

      <template v-if="!trainingStore.loading && trainingStore.topicMasteries.length === 0 && trainingStore.skillTagMasteries.length === 0">
        <NbCard>
          <div class="tma-page__empty">
            <p>暂无掌握度数据，完成八股训练批改后这里会显示各知识点的掌握情况。</p>
          </div>
        </NbCard>
      </template>

      <div v-if="trainingStore.loading" class="tma-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useTrainingStore } from '@/stores/training'
import { MASTERY_LEVEL_OPTIONS } from '@/types/training'
import type { MasteryLevel } from '@/types/training'

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

function masteryColor(level: MasteryLevel) {
  return MASTERY_LEVEL_OPTIONS.find(o => o.value === level)?.color || '#999'
}

function masteryLabel(level: MasteryLevel) {
  return MASTERY_LEVEL_OPTIONS.find(o => o.value === level)?.label || level
}

function formatTime(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.tma-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tma-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tma-page__title {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.tma-page__summary-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.tma-page__summary-card {
  text-align: center;
  padding: 20px 16px;
}

.tma-page__summary-value {
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 700;
  color: var(--nb-primary);
  margin-bottom: 4px;
}

.tma-page__summary-value--danger { color: #e74c3c; }
.tma-page__summary-value--warning { color: #f39c12; }
.tma-page__summary-value--success { color: #2ecc71; }
.tma-page__summary-value--purple { color: #6C5CE7; }

.tma-page__summary-label {
  font-size: 13px;
  color: var(--nb-muted);
}

.tma-page__section-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.tma-page__table {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.tma-page__table-head {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: var(--nb-bg);
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
.tma-page__cell-weak { flex: 0 0 60px; text-align: center; color: #e74c3c; font-weight: 600; }
.tma-page__cell-mastered { flex: 0 0 70px; text-align: center; color: #6C5CE7; font-weight: 600; }
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

.tma-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.tma-page__empty {
  text-align: center;
  padding: 40px 0;
  color: var(--nb-muted);
  font-size: 15px;
}

@media (max-width: 768px) {
  .tma-page__summary-row {
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

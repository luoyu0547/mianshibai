<template>
  <MainLayout>
    <div class="diagnosis-page">
      <NbPageHeader
        eyebrow="Coach"
        title="求职诊断报告"
      >
        <template #actions>
          <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="coachStore.loading">
        <NbLoadingBlock title="加载诊断报告..." :rows="5" />
      </NbCard>

      <template v-else-if="coachStore.currentDiagnosis">
        <NbCard variant="ai">
          <div class="diagnosis-header">
            <div class="diagnosis-header__main">
              <h2 class="diagnosis-header__title">{{ coachStore.currentDiagnosis.title }}</h2>
              <p class="diagnosis-header__meta">
                {{ formatDateTime(coachStore.currentDiagnosis.createTime) }}
                · {{ coachStore.currentDiagnosis.source === 'ai' ? 'AI 生成' : '规则兜底' }}
              </p>
            </div>
            <div class="diagnosis-header__score">
              <span class="diagnosis-header__score-value">{{ coachStore.currentDiagnosis.overallScore }}</span>
              <span class="diagnosis-header__score-label">综合评分</span>
            </div>
          </div>
          <p class="diagnosis-summary">{{ coachStore.currentDiagnosis.summary }}</p>
        </NbCard>

        <NbCard>
          <NbSectionTitle title="数据完整度" />
          <div class="completeness">
            <el-progress
              :percentage="coachStore.currentDiagnosis.dataCompleteness"
              :stroke-width="14"
              :format="() => `${coachStore.currentDiagnosis!.dataCompleteness}%`"
            />
            <p class="completeness__hint">
              数据越完整，诊断结果越准确。建议补全简历、面试和投递记录以获得更精准的分析。
            </p>
          </div>
        </NbCard>

        <NbCard>
          <NbSectionTitle title="优势" description="你的核心竞争力" />
          <div v-if="coachStore.currentDiagnosis.strengths.length" class="item-list">
            <NbCard
              v-for="(item, index) in coachStore.currentDiagnosis.strengths"
              :key="index"
              variant="success"
              compact
            >
              <p class="item-text">{{ item }}</p>
            </NbCard>
          </div>
          <NbEmptyState v-else title="暂无优势分析" description="数据不足，暂无法分析优势。" />
        </NbCard>

        <NbCard>
          <NbSectionTitle title="短板" description="需要重点提升的方面" />
          <div v-if="coachStore.currentDiagnosis.weaknesses.length" class="item-list">
            <NbCard
              v-for="(item, index) in coachStore.currentDiagnosis.weaknesses"
              :key="index"
              variant="warning"
              compact
            >
              <p class="item-text">{{ item }}</p>
            </NbCard>
          </div>
          <NbEmptyState v-else title="暂无短板分析" description="数据不足，暂无法分析短板。" />
        </NbCard>

        <NbCard variant="ai">
          <NbSectionTitle title="改进建议" description="AI 生成的行动建议" />
          <div v-if="coachStore.currentDiagnosis.suggestions.length" class="item-list">
            <NbCard
              v-for="(item, index) in coachStore.currentDiagnosis.suggestions"
              :key="index"
              compact
              class="suggestion-card"
            >
              <div class="suggestion">
                <span class="suggestion__num">{{ index + 1 }}</span>
                <p class="item-text">{{ item }}</p>
              </div>
            </NbCard>
          </div>
          <NbEmptyState v-else title="暂无建议" description="数据不足，暂无法生成建议。" />
        </NbCard>
      </template>

      <NbCard v-else>
        <NbEmptyState title="诊断报告不存在" description="该诊断可能已被删除。">
          <template #action>
            <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useCoachStore } from '@/stores/coach'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const coachStore = useCoachStore()

onMounted(() => coachStore.fetchDiagnosis(Number(route.params.id)))
</script>

<style scoped>
.diagnosis-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.diagnosis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.diagnosis-header__main {
  min-width: 0;
}

.diagnosis-header__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
}

.diagnosis-header__meta {
  margin: 6px 0 0;
  color: var(--nb-muted);
  font-size: 14px;
}

.diagnosis-header__score {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100px;
  padding: 16px 12px;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius-lg);
  background: var(--nb-primary);
  color: #fff;
}

.diagnosis-header__score-value {
  font-family: var(--font-heading);
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
}

.diagnosis-header__score-label {
  font-size: 12px;
  opacity: 0.9;
}

.diagnosis-summary {
  margin: 0;
  color: var(--nb-muted);
  font-size: 15px;
  line-height: 1.6;
}

.completeness {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.completeness__hint {
  margin: 0;
  font-size: 13px;
  color: var(--nb-muted);
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.item-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}

.suggestion-card {
  background: var(--nb-surface);
}

.suggestion {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.suggestion__num {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-primary);
  color: #fff;
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .diagnosis-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

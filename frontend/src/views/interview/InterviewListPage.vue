<template>
  <MainLayout>
    <div class="interview-list-page">
      <NbPageHeader
        eyebrow="面试模拟"
        title="模拟面试记录"
        description="回顾你的模拟面试表现，持续提升实战能力"
      >
        <template #actions>
          <NbButton variant="primary" @click="router.push('/interview/new')">+ 开始新面试</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="interviewStore.loading">
        <NbLoadingBlock title="加载面试记录..." :rows="4" />
      </NbCard>

      <NbCard v-else-if="interviewStore.sessions.length === 0">
        <NbEmptyState
          title="还没有面试记录"
          description="立即开始一场模拟面试，体验真实的 AI 面试官"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/interview/new')">开始面试</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <div v-else class="interview-list-page__grid">
        <NbCard
          v-for="session in interviewStore.sessions"
          :key="session.id"
          hoverable
          class="interview-card"
        >
          <div class="interview-card__header">
            <h3 class="interview-card__title">{{ session.title }}</h3>
            <NbStatusBadge
              :label="getStatusDescriptor(interviewStatusMap, session.status).label"
              :variant="getStatusDescriptor(interviewStatusMap, session.status).variant"
            />
          </div>
          <div class="interview-card__meta">
            <NbStatusBadge
              :label="session.targetPosition"
              variant="info"
            />
            <span v-if="session.techDirection" class="interview-card__tech">{{ session.techDirection }}</span>
          </div>
          <div class="interview-card__progress-row">
            <span class="interview-card__progress-label">进度</span>
            <span class="interview-card__progress-value">
              {{ session.currentQuestionNo }} / {{ session.totalQuestions }} 题
            </span>
            <span
              v-if="session.durationMinutes"
              class="interview-card__duration"
            >{{ session.durationMinutes }} 分钟</span>
          </div>
          <div class="interview-card__info">
            <span class="interview-card__time">{{ formatDateTime(session.updateTime) }}</span>
          </div>
          <div class="interview-card__actions">
            <NbButton
              v-if="session.status === 'in_progress'"
              variant="primary"
              @click="router.push(`/interview/${session.id}/room`)"
            >
              继续面试
            </NbButton>
            <NbButton
              v-if="session.status === 'completed'"
              variant="primary"
              @click="router.push(`/interview/${session.id}/report`)"
            >
              查看报告
            </NbButton>
            <NbButton
              v-if="session.status === 'created' || session.status === 'in_progress'"
              variant="danger"
              @click="handleCancel(session.id, session.title)"
            >
              取消
            </NbButton>
          </div>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import { useInterviewStore } from '@/stores/interview'
import { interviewStatusMap, getStatusDescriptor } from '@/utils/statusMaps'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const interviewStore = useInterviewStore()

onMounted(() => {
  interviewStore.fetchSessions()
})

async function handleCancel(id: number, title: string) {
  try {
    await ElMessageBox.confirm(`确定取消面试「${title}」吗？`, '确认取消', {
      confirmButtonText: '取消面试',
      cancelButtonText: '返回',
      type: 'warning',
    })
    const success = await interviewStore.cancelSession(id)
    if (success) {
      ElMessage.success('已取消')
    }
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.interview-list-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.interview-list-page__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.interview-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.interview-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
  margin-right: 8px;
}

.interview-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.interview-card__tech {
  font-size: 13px;
  color: var(--nb-muted);
}

.interview-card__progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.interview-card__progress-label {
  font-family: var(--font-heading);
  font-weight: 600;
  color: var(--nb-muted);
}

.interview-card__progress-value {
  color: var(--nb-ink);
  font-weight: 600;
}

.interview-card__duration {
  margin-left: auto;
  color: var(--nb-muted);
}

.interview-card__info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--nb-muted);
}

.interview-card__actions {
  display: flex;
  gap: 8px;
  border-top: 2px solid var(--nb-bg);
  padding-top: 12px;
}
</style>

<template>
  <MainLayout>
    <div class="interview-list-page">
      <div class="interview-list-page__header">
        <h1 class="interview-list-page__title">模拟面试记录</h1>
        <NbButton type="primary" @click="router.push('/interview/new')">+ 开始新面试</NbButton>
      </div>

      <div v-if="interviewStore.loading" class="interview-list-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="interviewStore.sessions.length === 0" class="interview-list-page__empty">
        <div class="interview-list-page__empty-icon">🎙️</div>
        <p class="interview-list-page__empty-text">还没有面试记录，立即开始吧！</p>
        <NbButton type="primary" @click="router.push('/interview/new')">开始面试</NbButton>
      </div>

      <div v-else class="interview-list-page__grid">
        <NbCard
          v-for="session in interviewStore.sessions"
          :key="session.id"
          hoverable
          class="interview-card"
        >
          <div class="interview-card__header">
            <h3 class="interview-card__title">{{ session.title }}</h3>
            <el-tag
              class="interview-card__badge"
              :type="statusTagType(session.status)"
              size="small"
            >
              {{ statusLabel(session.status) }}
            </el-tag>
          </div>
          <div class="interview-card__meta">
            <el-tag size="small" effect="plain">{{ session.targetPosition }}</el-tag>
            <span v-if="session.techDirection" class="interview-card__tech">{{ session.techDirection }}</span>
          </div>
          <div class="interview-card__info">
            <span v-if="session.status === 'completed' && session.totalQuestions > 0" class="interview-card__score">
              综合评分：-- 分
            </span>
            <span class="interview-card__time">{{ formatTime(session.updateTime) }}</span>
          </div>
          <div class="interview-card__actions">
            <el-button
              v-if="session.status === 'in_progress'"
              type="primary"
              text
              @click="router.push(`/interview/${session.id}/room`)"
            >
              继续面试
            </el-button>
            <el-button
              v-if="session.status === 'completed'"
              type="primary"
              text
              @click="router.push(`/interview/${session.id}/report`)"
            >
              查看报告
            </el-button>
            <el-button
              v-if="session.status === 'created' || session.status === 'in_progress'"
              type="danger"
              text
              @click="handleCancel(session.id, session.title)"
            >
              取消
            </el-button>
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
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useInterviewStore } from '@/stores/interview'
import type { InterviewStatus } from '@/types/interview'

const router = useRouter()
const interviewStore = useInterviewStore()

onMounted(() => {
  interviewStore.fetchSessions()
})

function statusLabel(status: InterviewStatus) {
  const map: Record<InterviewStatus, string> = {
    created: '待开始',
    in_progress: '进行中',
    generating_report: '生成报告中',
    completed: '已完成',
    cancelled: '已取消',
  }
  return map[status] || status
}

function statusTagType(status: InterviewStatus) {
  const map: Record<InterviewStatus, string> = {
    created: 'info',
    in_progress: '',
    generating_report: 'warning',
    completed: 'success',
    cancelled: 'danger',
  }
  return (map[status] || 'info') as '' | 'success' | 'warning' | 'info' | 'danger'
}

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

function formatTime(timeStr: string) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.interview-list-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.interview-list-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.interview-list-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.interview-list-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.interview-list-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 0;
}

.interview-list-page__empty-icon {
  font-size: 64px;
}

.interview-list-page__empty-text {
  font-size: 16px;
  color: var(--nb-muted);
  margin: 0;
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

.interview-card__badge {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
  flex-shrink: 0;
}

.interview-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.interview-card__tech {
  font-size: 13px;
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

.interview-card__score {
  font-weight: 600;
  color: var(--nb-primary);
}

.interview-card__actions {
  display: flex;
  gap: 8px;
  border-top: 2px solid var(--nb-bg);
  padding-top: 12px;
}
</style>

<!-- src/views/application/ApplicationDetailPage.vue -->
<template>
  <MainLayout>
    <div class="app-detail-page">
      <div v-if="applicationStore.loading" class="app-detail-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="app">
        <div class="app-detail-page__header">
          <el-button text @click="router.back()">&larr; 返回</el-button>
        </div>

        <div class="app-detail-page__hero">
          <div class="app-detail-page__hero-left">
            <h1 class="app-detail-page__title">{{ app.jobTitle }}</h1>
            <div class="app-detail-page__meta">
              <span>{{ app.companyName }}</span>
              <span class="app-detail-page__meta-divider">|</span>
              <span>{{ app.location }}</span>
              <template v-if="app.salaryRange">
                <span class="app-detail-page__meta-divider">|</span>
                <span class="app-detail-page__salary">{{ app.salaryRange }}</span>
              </template>
            </div>
          </div>
          <div class="app-detail-page__hero-actions">
            <el-select
              :model-value="app.status"
              style="width: 140px;"
              @change="(val: ApplicationStatus) => handleStatusChange(val)"
            >
              <el-option
                v-for="opt in APPLICATION_STATUS_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
        </div>

        <NbCard>
          <h3 class="app-detail-page__section-title">基本信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="来源">{{ app.source || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ app.statusLabel }}</el-descriptions-item>
            <el-descriptions-item label="投递日期">{{ formatDate(app.appliedAt) }}</el-descriptions-item>
            <el-descriptions-item label="下一事件">{{ formatDate(app.nextEventAt) }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ app.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ app.contactInfo || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="app.notes" class="app-detail-page__notes">
            <strong>备注：</strong>{{ app.notes }}
          </div>
        </NbCard>

        <div class="app-detail-page__links">
          <el-button v-if="app.jobId" text type="primary" @click="router.push(`/job/${app.jobId}`)">
            查看关联职位
          </el-button>
          <el-button v-if="app.resumeId" text type="primary" @click="router.push(`/resume/${app.resumeId}/preview`)">
            查看关联简历
          </el-button>
        </div>

        <NbCard>
          <div class="app-detail-page__todo-header">
            <h3 class="app-detail-page__section-title" style="margin: 0;">待办事项</h3>
          </div>

          <div class="app-detail-page__todo-form">
            <el-input v-model="newTodo.title" placeholder="待办标题" style="flex: 1;" />
            <el-select v-model="newTodo.priority" style="width: 100px;">
              <el-option
                v-for="opt in TODO_PRIORITY_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-date-picker
              v-model="newTodo.dueAt"
              type="date"
              placeholder="截止日期"
              value-format="YYYY-MM-DD"
              style="width: 160px;"
            />
            <NbButton type="primary" @click="handleCreateTodo">添加</NbButton>
          </div>

          <div v-if="app.todos && app.todos.length > 0" class="app-detail-page__todo-list">
            <div
              v-for="todo in app.todos"
              :key="todo.id"
              :class="['app-detail-page__todo-item', { 'app-detail-page__todo-item--done': todo.completed }]"
            >
              <div class="app-detail-page__todo-content">
                <span class="app-detail-page__todo-title">{{ todo.title }}</span>
                <el-tag
                  :type="priorityTagType(todo.priority)"
                  size="small"
                  class="app-detail-page__priority-tag"
                >
                  {{ todo.priorityLabel }}
                </el-tag>
                <span v-if="todo.dueAt" class="app-detail-page__todo-due">
                  截止: {{ formatDate(todo.dueAt) }}
                </span>
              </div>
              <div class="app-detail-page__todo-actions">
                <el-button
                  v-if="!todo.completed"
                  type="success"
                  size="small"
                  @click="handleCompleteTodo(todo.id)"
                >
                  完成
                </el-button>
                <el-button
                  v-else
                  size="small"
                  @click="handleReopenTodo(todo.id)"
                >
                  重开
                </el-button>
              </div>
            </div>
          </div>
          <div v-else class="app-detail-page__todo-empty">
            暂无待办事项
          </div>
        </NbCard>
      </template>

      <div v-else class="app-detail-page__empty">
        <p>未找到该投递记录</p>
        <el-button type="primary" text @click="router.push('/applications')">返回列表</el-button>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useApplicationStore } from '@/stores/application'
import { APPLICATION_STATUS_OPTIONS, TODO_PRIORITY_OPTIONS } from '@/types/application'
import type { ApplicationStatus, TodoPriority } from '@/types/application'

const route = useRoute()
const router = useRouter()
const applicationStore = useApplicationStore()

const app = computed(() => applicationStore.currentApplication)

const newTodo = reactive({
  title: '',
  priority: 'medium' as TodoPriority,
  dueAt: null as string | null,
})

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    applicationStore.fetchApplication(id)
  }
})

async function handleStatusChange(status: ApplicationStatus) {
  const id = Number(route.params.id)
  const result = await applicationStore.updateApplicationStatus(id, status)
  if (result) {
    ElMessage.success('状态已更新')
  } else {
    ElMessage.error('状态更新失败')
  }
}

async function handleCreateTodo() {
  if (!newTodo.title.trim()) {
    ElMessage.warning('请输入待办标题')
    return
  }
  const id = Number(route.params.id)
  const result = await applicationStore.createApplicationTodo(id, {
    title: newTodo.title,
    priority: newTodo.priority,
    dueAt: newTodo.dueAt,
  })
  if (result) {
    ElMessage.success('待办已添加')
    newTodo.title = ''
    newTodo.dueAt = null
    applicationStore.fetchApplication(id)
  }
}

async function handleCompleteTodo(todoId: number) {
  const ok = await applicationStore.completeTodo(todoId)
  if (ok) {
    ElMessage.success('已标记完成')
    applicationStore.fetchApplication(Number(route.params.id))
  }
}

async function handleReopenTodo(todoId: number) {
  const ok = await applicationStore.reopenTodo(todoId)
  if (ok) {
    ElMessage.success('已重新打开')
    applicationStore.fetchApplication(Number(route.params.id))
  }
}

function formatDate(dateStr: string | null | undefined) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function priorityTagType(priority: TodoPriority) {
  if (priority === 'high') return 'danger'
  if (priority === 'medium') return 'warning'
  return 'info'
}
</script>

<style scoped>
.app-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.app-detail-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.app-detail-page__header {
  display: flex;
  align-items: center;
}

.app-detail-page__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.app-detail-page__hero-left {
  flex: 1;
}

.app-detail-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.app-detail-page__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--nb-muted);
  font-size: 15px;
}

.app-detail-page__meta-divider {
  color: var(--nb-border);
}

.app-detail-page__salary {
  color: var(--nb-accent);
  font-weight: 600;
}

.app-detail-page__hero-actions {
  flex-shrink: 0;
  display: flex;
  gap: 12px;
}

.app-detail-page__section-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.app-detail-page__notes {
  margin-top: 16px;
  line-height: 1.6;
  color: var(--nb-text);
}

.app-detail-page__links {
  display: flex;
  gap: 12px;
}

.app-detail-page__todo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.app-detail-page__todo-form {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.app-detail-page__todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-detail-page__todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-bg);
}

.app-detail-page__todo-item--done {
  opacity: 0.6;
}

.app-detail-page__todo-item--done .app-detail-page__todo-title {
  text-decoration: line-through;
}

.app-detail-page__todo-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  flex-wrap: wrap;
}

.app-detail-page__todo-title {
  font-weight: 500;
}

.app-detail-page__priority-tag {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
}

.app-detail-page__todo-due {
  font-size: 13px;
  color: var(--nb-muted);
}

.app-detail-page__todo-actions {
  flex-shrink: 0;
}

.app-detail-page__todo-empty {
  text-align: center;
  color: var(--nb-muted);
  padding: 24px 0;
}

.app-detail-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}
</style>

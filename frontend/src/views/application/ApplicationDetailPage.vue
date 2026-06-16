<!-- src/views/application/ApplicationDetailPage.vue -->
<template>
  <MainLayout>
    <div class="app-detail-page">
      <NbCard v-if="applicationStore.loading">
        <NbLoadingBlock title="加载投递详情..." :rows="6" />
      </NbCard>

      <template v-else-if="app">
        <NbButton variant="ghost" @click="router.back()">&larr; 返回</NbButton>

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
            <NbStatusBadge
              :label="getStatusDescriptor(applicationStatusMap, app.status).label"
              :variant="getStatusDescriptor(applicationStatusMap, app.status).variant"
            />
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
          <NbSectionTitle title="基本信息" />
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
          <NbButton v-if="app.resumeId" variant="ghost" @click="router.push(`/resume/${app.resumeId}/preview`)">
            查看关联简历
          </NbButton>
        </div>

        <div class="app-detail-page__actions">
          <NbButton variant="primary" @click="showRoundDialog = true">添加面试轮次</NbButton>
        </div>

        <div class="app-detail-page__rounds">
          <div v-for="(round, index) in rounds" :key="round.id" class="app-detail-page__round-card">
            <div class="app-detail-page__round-index">{{ index + 1 }}</div>
            <div class="app-detail-page__round-body">
              <div class="app-detail-page__round-header">
                <span class="app-detail-page__round-name">{{ round.roundName }}</span>
                <NbStatusBadge
                  :label="roundResultLabel(round.result)"
                  :variant="roundResultVariant(round.result)"
                />
              </div>
              <div v-if="round.scheduledAt" class="app-detail-page__round-time">
                面试时间：{{ formatDate(round.scheduledAt) }}
              </div>
              <div v-if="round.notes" class="app-detail-page__round-notes">{{ round.notes }}</div>
              <div class="app-detail-page__round-actions">
                <NbButton
                  v-if="round.result === 'pending'"
                  size="small"
                  variant="success"
                  @click="handleRoundResult(round.id, 'pass')"
                >标记通过</NbButton>
                <NbButton
                  v-if="round.result === 'pending'"
                  size="small"
                  variant="danger"
                  @click="handleRoundResult(round.id, 'fail')"
                >标记淘汰</NbButton>
                <NbButton size="small" variant="ghost" @click="editRound(round)">编辑</NbButton>
                <NbButton size="small" variant="ghost" @click="handleDeleteRound(round.id)">删除</NbButton>
              </div>
            </div>
          </div>
          <div v-if="rounds.length === 0" class="app-detail-page__rounds-empty">
            暂无面试轮次，点击上方按钮添加
          </div>
        </div>

        <NbCard>
          <NbSectionTitle title="待办事项" />

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
            <NbButton variant="primary" @click="handleCreateTodo">添加</NbButton>
          </div>

          <div v-if="app.todos && app.todos.length > 0" class="app-detail-page__todo-list">
            <div
              v-for="todo in app.todos"
              :key="todo.id"
              :class="['app-detail-page__todo-item', { 'app-detail-page__todo-item--done': todo.completed }]"
            >
              <div class="app-detail-page__todo-content">
                <span class="app-detail-page__todo-title">{{ todo.title }}</span>
                <NbStatusBadge
                  :label="getStatusDescriptor(todoPriorityMap, todo.priority).label"
                  :variant="getStatusDescriptor(todoPriorityMap, todo.priority).variant"
                />
                <span v-if="todo.dueAt" class="app-detail-page__todo-due">
                  截止: {{ formatDate(todo.dueAt) }}
                </span>
              </div>
              <div class="app-detail-page__todo-actions">
                <NbButton
                  v-if="!todo.completed"
                  variant="success"
                  @click="handleCompleteTodo(todo.id)"
                >
                  完成
                </NbButton>
                <NbButton
                  v-else
                  variant="ghost"
                  @click="handleReopenTodo(todo.id)"
                >
                  重开
                </NbButton>
              </div>
            </div>
          </div>
          <div v-else class="app-detail-page__todo-empty">
            暂无待办事项
          </div>
        </NbCard>

        <el-dialog v-model="showRoundDialog" :title="editingRound ? '编辑轮次' : '添加轮次'" width="480px" destroy-on-close>
          <el-form :model="roundForm" label-position="top">
    <el-form-item label="轮次名称" required>
      <el-select v-model="roundForm.roundName" placeholder="选择轮次类型" allow-create filterable style="width: 100%;">
        <el-option
          v-for="opt in ROUND_NAME_OPTIONS"
          :key="opt"
          :label="opt"
          :value="opt"
        />
      </el-select>
    </el-form-item>
            <el-form-item label="面试时间">
              <el-date-picker
                v-model="roundForm.scheduledAt"
                type="datetime"
                placeholder="选择面试时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%;"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="roundForm.notes" type="textarea" :rows="3" placeholder="备注信息" />
            </el-form-item>
          </el-form>
          <template #footer>
            <NbButton variant="ghost" @click="showRoundDialog = false">取消</NbButton>
            <NbButton variant="primary" :loading="roundSaving" @click="saveRound">保存</NbButton>
          </template>
        </el-dialog>
      </template>

      <NbCard v-else>
        <NbEmptyState title="未找到该投递记录">
          <template #action>
            <NbButton variant="primary" @click="router.push('/applications')">返回列表</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useApplicationStore } from '@/stores/application'
import { APPLICATION_STATUS_OPTIONS, TODO_PRIORITY_OPTIONS } from '@/types/application'
import type { ApplicationStatus, TodoPriority } from '@/types/application'
import { applicationStatusMap, todoPriorityMap, getStatusDescriptor } from '@/utils/statusMaps'
import { formatDate } from '@/utils/date'
import {
  listApplicationRounds,
  createApplicationRound,
  updateApplicationRound,
  setApplicationRoundResult,
  deleteApplicationRound,
} from '@/api/application'
import type { ApplicationRoundVO, RoundResult } from '@/types/application'

const ROUND_NAME_OPTIONS = [
  'HR 沟通',
  '笔试',
  '技术一面',
  '技术二面',
  '技术三面',
  '主管面',
  'HR 面',
  '交叉面',
  '加签面试',
  '英语面试',
  '群体面试',
  '终面',
]

const route = useRoute()
const router = useRouter()
const applicationStore = useApplicationStore()

const app = computed(() => applicationStore.currentApplication)

const newTodo = reactive({
  title: '',
  priority: 'medium' as TodoPriority,
  dueAt: null as string | null,
})

const rounds = ref<ApplicationRoundVO[]>([])
const showRoundDialog = ref(false)
const roundSaving = ref(false)
const editingRound = ref<ApplicationRoundVO | null>(null)

const roundForm = reactive({
  roundName: '',
  scheduledAt: null as string | null,
  notes: '',
})

watch(showRoundDialog, (val) => {
  if (!val) {
    editingRound.value = null
    roundForm.roundName = ''
    roundForm.scheduledAt = null
    roundForm.notes = ''
  } else if (editingRound.value) {
    roundForm.roundName = editingRound.value.roundName
    roundForm.scheduledAt = editingRound.value.scheduledAt
    roundForm.notes = editingRound.value.notes || ''
  }
})

function roundResultVariant(result: RoundResult): 'success' | 'danger' | 'muted' {
  return result === 'pass' ? 'success' : result === 'fail' ? 'danger' : 'muted'
}

function roundResultLabel(result: RoundResult): string {
  return result === 'pass' ? '通过' : result === 'fail' ? '淘汰' : '待定'
}

async function loadRounds() {
  const id = Number(route.params.id)
  const res = await listApplicationRounds(id)
  if (res.code === 0 && res.data) {
    rounds.value = res.data
  }
}

async function saveRound() {
  if (!roundForm.roundName.trim()) return
  roundSaving.value = true
  try {
    const id = Number(route.params.id)
    if (editingRound.value) {
      await updateApplicationRound(id, editingRound.value.id, {
        roundName: roundForm.roundName,
        scheduledAt: roundForm.scheduledAt,
        notes: roundForm.notes || undefined,
      })
    } else {
      await createApplicationRound(id, {
        roundName: roundForm.roundName,
        scheduledAt: roundForm.scheduledAt,
        notes: roundForm.notes || undefined,
      })
    }
    showRoundDialog.value = false
    await loadRounds()
    applicationStore.fetchApplication(id)
  } finally {
    roundSaving.value = false
  }
}

function editRound(round: ApplicationRoundVO) {
  editingRound.value = round
  showRoundDialog.value = true
}

async function handleRoundResult(roundId: number, result: RoundResult) {
  const id = Number(route.params.id)
  if (result === 'fail') {
    try {
      await ElMessageBox.confirm('标记淘汰后投递状态将自动变为"已拒绝"，确认？', '确认淘汰', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await setApplicationRoundResult(id, roundId, { result })
      await loadRounds()
      applicationStore.fetchApplication(id)
    } catch {
      // cancelled by user
    }
  } else {
    await setApplicationRoundResult(id, roundId, { result })
    await loadRounds()
  }
}

async function handleDeleteRound(roundId: number) {
  const id = Number(route.params.id)
  await deleteApplicationRound(id, roundId)
  await loadRounds()
}

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    applicationStore.fetchApplication(id)
    loadRounds()
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
</script>

<style scoped>
.app-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
  align-items: center;
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

.app-detail-page__actions {
  display: flex;
  justify-content: flex-end;
}

.app-detail-page__rounds {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-detail-page__round-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 2px solid var(--nb-border-color, #e5e7eb);
  border-radius: 8px;
  background: var(--nb-bg, #fff);
}

.app-detail-page__round-index {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--nb-primary, #6C5CE7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.app-detail-page__round-body {
  flex: 1;
  min-width: 0;
}

.app-detail-page__round-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.app-detail-page__round-name {
  font-weight: 600;
  font-size: 16px;
}

.app-detail-page__round-time {
  font-size: 13px;
  color: var(--nb-muted, #6b7280);
  margin-bottom: 6px;
}

.app-detail-page__round-notes {
  font-size: 14px;
  line-height: 1.5;
  color: var(--nb-text, #374151);
  margin-bottom: 8px;
}

.app-detail-page__round-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.app-detail-page__rounds-empty {
  text-align: center;
  color: var(--nb-muted, #6b7280);
  padding: 24px 0;
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
</style>

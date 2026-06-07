<!-- src/views/application/ApplicationTodoPage.vue -->
<template>
  <MainLayout>
    <div class="app-todo-page">
      <div class="app-todo-page__header">
        <h1 class="app-todo-page__title">待办中心</h1>
        <NbButton @click="router.push('/applications')">返回投递列表</NbButton>
      </div>

      <NbCard>
        <div class="app-todo-page__create">
          <el-input v-model="newTodo.title" placeholder="新增待办" style="flex: 1;" />
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
          <NbButton type="primary" @click="handleCreateGlobalTodo">添加</NbButton>
        </div>
      </NbCard>

      <div class="app-todo-page__filter">
        <el-select
          v-model="completedFilter"
          placeholder="完成状态"
          clearable
          style="width: 140px;"
          @change="loadTodos"
        >
          <el-option label="未完成" :value="false" />
          <el-option label="已完成" :value="true" />
        </el-select>
        <el-select
          v-model="priorityFilter"
          placeholder="优先级"
          clearable
          style="width: 120px;"
          @change="loadTodos"
        >
          <el-option
            v-for="opt in TODO_PRIORITY_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>

      <div v-if="applicationStore.loading" class="app-todo-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="applicationStore.todos.length === 0" class="app-todo-page__empty">
        <div class="app-todo-page__empty-icon">✅</div>
        <p class="app-todo-page__empty-text">暂无待办事项</p>
      </div>

      <div v-else class="app-todo-page__list">
        <div
          v-for="todo in applicationStore.todos"
          :key="todo.id"
          :class="['app-todo-page__item', { 'app-todo-page__item--done': todo.completed }]"
        >
          <div class="app-todo-page__item-main">
            <span class="app-todo-page__item-title">{{ todo.title }}</span>
            <el-tag
              :type="priorityTagType(todo.priority)"
              size="small"
              class="app-todo-page__priority-tag"
            >
              {{ todo.priorityLabel }}
            </el-tag>
            <span v-if="todo.dueAt" class="app-todo-page__item-due">
              截止: {{ formatDate(todo.dueAt) }}
            </span>
            <span v-if="todo.completed && todo.completedAt" class="app-todo-page__item-done">
              完成: {{ formatDate(todo.completedAt) }}
            </span>
          </div>
          <div v-if="todo.applicationCompanyName || todo.applicationJobTitle" class="app-todo-page__item-app">
            <el-link
              v-if="todo.applicationId"
              type="primary"
              @click="router.push(`/applications/${todo.applicationId}`)"
            >
              {{ todo.applicationCompanyName }} - {{ todo.applicationJobTitle }}
            </el-link>
            <span v-else>{{ todo.applicationCompanyName }} - {{ todo.applicationJobTitle }}</span>
          </div>
          <div class="app-todo-page__item-actions">
            <el-button
              v-if="!todo.completed"
              type="success"
              size="small"
              @click="handleComplete(todo.id)"
            >
              完成
            </el-button>
            <el-button
              v-else
              size="small"
              @click="handleReopen(todo.id)"
            >
              重开
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(todo.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useApplicationStore } from '@/stores/application'
import { TODO_PRIORITY_OPTIONS } from '@/types/application'
import type { TodoPriority } from '@/types/application'

const router = useRouter()
const applicationStore = useApplicationStore()

const completedFilter = ref<boolean | undefined>(undefined)
const priorityFilter = ref<TodoPriority | ''>('')

const newTodo = reactive({
  title: '',
  priority: 'medium' as TodoPriority,
  dueAt: null as string | null,
})

onMounted(() => {
  loadTodos()
})

async function loadTodos() {
  await applicationStore.fetchTodos({
    completed: completedFilter.value,
    priority: priorityFilter.value || undefined,
  })
}

async function handleCreateGlobalTodo() {
  if (!newTodo.title.trim()) {
    ElMessage.warning('请输入待办标题')
    return
  }
  const result = await applicationStore.createGlobalTodo({
    title: newTodo.title,
    priority: newTodo.priority,
    dueAt: newTodo.dueAt,
  })
  if (result) {
    ElMessage.success('待办已添加')
    newTodo.title = ''
    newTodo.dueAt = null
    loadTodos()
  }
}

async function handleComplete(id: number) {
  const ok = await applicationStore.completeTodo(id)
  if (ok) {
    ElMessage.success('已标记完成')
    loadTodos()
  }
}

async function handleReopen(id: number) {
  const ok = await applicationStore.reopenTodo(id)
  if (ok) {
    ElMessage.success('已重新打开')
    loadTodos()
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该待办？', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const ok = await applicationStore.deleteTodo(id)
    if (ok) {
      ElMessage.success('已删除')
      loadTodos()
    }
  } catch {
    // cancelled
  }
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function priorityTagType(priority: TodoPriority) {
  if (priority === 'high') return 'danger'
  if (priority === 'medium') return 'warning'
  return 'info'
}
</script>

<style scoped>
.app-todo-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.app-todo-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-todo-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.app-todo-page__create {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.app-todo-page__filter {
  display: flex;
  gap: 12px;
}

.app-todo-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.app-todo-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 0;
}

.app-todo-page__empty-icon {
  font-size: 64px;
}

.app-todo-page__empty-text {
  font-size: 16px;
  color: var(--nb-muted);
  margin: 0;
}

.app-todo-page__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-todo-page__item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-card);
  box-shadow: var(--nb-shadow);
}

.app-todo-page__item--done {
  opacity: 0.6;
}

.app-todo-page__item--done .app-todo-page__item-title {
  text-decoration: line-through;
}

.app-todo-page__item-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.app-todo-page__item-title {
  font-weight: 600;
  font-size: 16px;
}

.app-todo-page__priority-tag {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
}

.app-todo-page__item-due {
  font-size: 13px;
  color: var(--nb-muted);
}

.app-todo-page__item-done {
  font-size: 13px;
  color: var(--nb-success);
}

.app-todo-page__item-app {
  font-size: 14px;
  color: var(--nb-muted);
}

.app-todo-page__item-actions {
  display: flex;
  gap: 8px;
}
</style>

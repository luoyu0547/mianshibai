<!-- src/views/application/ApplicationTodoPage.vue -->
<template>
  <MainLayout>
    <div class="app-todo-page">
      <NbPageHeader
        eyebrow="求职管理"
        title="待办中心"
        description="统一管理所有投递相关的待办事项"
      >
        <template #actions>
          <NbButton variant="ghost" @click="router.push('/applications')">返回投递列表</NbButton>
        </template>
      </NbPageHeader>

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
          <NbButton variant="primary" @click="handleCreateGlobalTodo">添加</NbButton>
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

      <NbCard v-if="applicationStore.loading">
        <NbLoadingBlock title="加载待办..." :rows="4" />
      </NbCard>

      <NbCard v-else-if="applicationStore.todos.length === 0">
        <NbEmptyState title="暂无待办事项" description="所有待办都已清空，干得漂亮" />
      </NbCard>

      <div v-else class="app-todo-page__list">
        <NbCard
          v-for="todo in applicationStore.todos"
          :key="todo.id"
          :class="['app-todo-item', { 'app-todo-item--done': todo.completed }]"
        >
          <div class="app-todo-item__main">
            <span class="app-todo-item__title">{{ todo.title }}</span>
            <NbStatusBadge
              :label="getStatusDescriptor(todoPriorityMap, todo.priority).label"
              :variant="getStatusDescriptor(todoPriorityMap, todo.priority).variant"
            />
            <span v-if="todo.dueAt" class="app-todo-item__due">
              截止: {{ formatDate(todo.dueAt) }}
            </span>
            <span v-if="todo.completed && todo.completedAt" class="app-todo-item__done">
              完成: {{ formatDate(todo.completedAt) }}
            </span>
          </div>
          <div v-if="todo.applicationCompanyName || todo.applicationJobTitle" class="app-todo-item__app">
            <el-link
              v-if="todo.applicationId"
              type="primary"
              @click="router.push(`/applications/${todo.applicationId}`)"
            >
              {{ todo.applicationCompanyName }} - {{ todo.applicationJobTitle }}
            </el-link>
            <span v-else>{{ todo.applicationCompanyName }} - {{ todo.applicationJobTitle }}</span>
          </div>
          <div class="app-todo-item__actions">
            <NbButton
              v-if="!todo.completed"
              variant="success"
              @click="handleComplete(todo.id)"
            >
              完成
            </NbButton>
            <NbButton
              v-else
              variant="ghost"
              @click="handleReopen(todo.id)"
            >
              重开
            </NbButton>
            <NbButton
              variant="danger"
              @click="handleDelete(todo.id)"
            >
              删除
            </NbButton>
          </div>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useApplicationStore } from '@/stores/application'
import { TODO_PRIORITY_OPTIONS } from '@/types/application'
import type { TodoPriority } from '@/types/application'
import { todoPriorityMap, getStatusDescriptor } from '@/utils/statusMaps'
import { formatDate } from '@/utils/date'

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
</script>

<style scoped>
.app-todo-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
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

.app-todo-page__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-todo-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.app-todo-item--done {
  opacity: 0.6;
}

.app-todo-item--done .app-todo-item__title {
  text-decoration: line-through;
}

.app-todo-item__main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.app-todo-item__title {
  font-weight: 600;
  font-size: 16px;
}

.app-todo-item__due {
  font-size: 13px;
  color: var(--nb-muted);
}

.app-todo-item__done {
  font-size: 13px;
  color: var(--nb-success);
}

.app-todo-item__app {
  font-size: 14px;
  color: var(--nb-muted);
}

.app-todo-item__actions {
  display: flex;
  gap: 8px;
}
</style>

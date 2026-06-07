import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createApplication as createApplicationApi,
  listApplications as listApplicationsApi,
  getApplicationStats as getApplicationStatsApi,
  getApplication as getApplicationApi,
  updateApplication as updateApplicationApi,
  updateApplicationStatus as updateApplicationStatusApi,
  deleteApplication as deleteApplicationApi,
  createApplicationTodo as createApplicationTodoApi,
  createGlobalTodo as createGlobalTodoApi,
  listApplicationTodos as listApplicationTodosApi,
  updateApplicationTodo as updateApplicationTodoApi,
  completeApplicationTodo as completeApplicationTodoApi,
  reopenApplicationTodo as reopenApplicationTodoApi,
  deleteApplicationTodo as deleteApplicationTodoApi,
} from '@/api/application'
import type {
  ApplicationCreateRequest,
  ApplicationListQueryRequest,
  ApplicationStatsVO,
  ApplicationStatus,
  ApplicationTodoCreateRequest,
  ApplicationTodoQueryRequest,
  ApplicationTodoUpdateRequest,
  ApplicationTodoVO,
  ApplicationUpdateRequest,
  JobApplicationVO,
} from '@/types/application'

export const useApplicationStore = defineStore('application', () => {
  const applications = ref<JobApplicationVO[]>([])
  const currentApplication = ref<JobApplicationVO | null>(null)
  const todos = ref<ApplicationTodoVO[]>([])
  const stats = ref<ApplicationStatsVO | null>(null)
  const loading = ref(false)

  async function fetchApplications(query?: ApplicationListQueryRequest) {
    loading.value = true
    try {
      const res = await listApplicationsApi(query)
      if (res.data.code === 0) {
        applications.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchStats() {
    loading.value = true
    try {
      const res = await getApplicationStatsApi()
      if (res.data.code === 0) {
        stats.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchApplication(id: number) {
    loading.value = true
    try {
      const res = await getApplicationApi(id)
      if (res.data.code === 0) {
        currentApplication.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createApplication(data: ApplicationCreateRequest) {
    loading.value = true
    try {
      const res = await createApplicationApi(data)
      if (res.data.code === 0) {
        return res.data.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function updateApplication(id: number, data: ApplicationUpdateRequest) {
    loading.value = true
    try {
      const res = await updateApplicationApi(id, data)
      if (res.data.code === 0) {
        currentApplication.value = res.data.data
        return res.data.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function updateApplicationStatus(id: number, status: ApplicationStatus) {
    loading.value = true
    try {
      const res = await updateApplicationStatusApi(id, { status })
      if (res.data.code === 0) {
        currentApplication.value = res.data.data
        return res.data.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function deleteApplication(id: number) {
    loading.value = true
    try {
      const res = await deleteApplicationApi(id)
      return res.data.code === 0
    } finally {
      loading.value = false
    }
  }

  async function createApplicationTodo(applicationId: number, data: ApplicationTodoCreateRequest) {
    const res = await createApplicationTodoApi(applicationId, data)
    if (res.data.code === 0) {
      return res.data.data
    }
    return null
  }

  async function createGlobalTodo(data: ApplicationTodoCreateRequest) {
    const res = await createGlobalTodoApi(data)
    if (res.data.code === 0) {
      return res.data.data
    }
    return null
  }

  async function fetchTodos(query?: ApplicationTodoQueryRequest) {
    loading.value = true
    try {
      const res = await listApplicationTodosApi(query)
      if (res.data.code === 0) {
        todos.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function updateTodo(id: number, data: ApplicationTodoUpdateRequest) {
    const res = await updateApplicationTodoApi(id, data)
    if (res.data.code === 0) {
      return res.data.data
    }
    return null
  }

  async function completeTodo(id: number) {
    const res = await completeApplicationTodoApi(id)
    return res.data.code === 0
  }

  async function reopenTodo(id: number) {
    const res = await reopenApplicationTodoApi(id)
    return res.data.code === 0
  }

  async function deleteTodo(id: number) {
    const res = await deleteApplicationTodoApi(id)
    return res.data.code === 0
  }

  return {
    applications,
    currentApplication,
    todos,
    stats,
    loading,
    fetchApplications,
    fetchStats,
    fetchApplication,
    createApplication,
    updateApplication,
    updateApplicationStatus,
    deleteApplication,
    createApplicationTodo,
    createGlobalTodo,
    fetchTodos,
    updateTodo,
    completeTodo,
    reopenTodo,
    deleteTodo,
  }
})

import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  disableAdminUser,
  enableAdminUser,
  getAdminOverview,
  getAdminUser,
  listAdminUsers,
  updateAdminUserRole,
} from '@/api/admin'
import type { AdminOverviewVO, AdminUserDetailVO, AdminUserListItemVO, AdminUserQueryRequest } from '@/types/admin'

export const useAdminStore = defineStore('admin', () => {
  const overview = ref<AdminOverviewVO | null>(null)
  const users = ref<AdminUserListItemVO[]>([])
  const currentUser = ref<AdminUserDetailVO | null>(null)
  const loading = ref(false)

  async function fetchOverview() {
    loading.value = true
    try {
      const res = await getAdminOverview()
      if (res.code === 0) {
        overview.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUsers(query?: AdminUserQueryRequest) {
    loading.value = true
    try {
      const res = await listAdminUsers(query)
      if (res.code === 0) {
        users.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUser(id: number) {
    loading.value = true
    try {
      const res = await getAdminUser(id)
      if (res.code === 0) {
        currentUser.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  async function disableUser(id: number) {
    const res = await disableAdminUser(id)
    if (res.code === 0) {
      currentUser.value = res.data
      return res.data
    }
    return null
  }

  async function enableUser(id: number) {
    const res = await enableAdminUser(id)
    if (res.code === 0) {
      currentUser.value = res.data
      return res.data
    }
    return null
  }

  async function updateUserRole(id: number, userRole: string) {
    const res = await updateAdminUserRole(id, { userRole })
    if (res.code === 0) {
      currentUser.value = res.data
      return res.data
    }
    return null
  }

  return {
    overview,
    users,
    currentUser,
    loading,
    fetchOverview,
    fetchUsers,
    fetchUser,
    disableUser,
    enableUser,
    updateUserRole,
  }
})

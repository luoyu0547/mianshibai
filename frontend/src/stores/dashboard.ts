import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getDashboard as getDashboardApi } from '@/api/dashboard'
import type { DashboardVO } from '@/types/training'

export const useDashboardStore = defineStore('dashboard', () => {
  const dashboard = ref<DashboardVO | null>(null)
  const loading = ref(false)

  async function fetchDashboard() {
    loading.value = true
    try {
      const res = await getDashboardApi()
      if (res.code === 0) {
        dashboard.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  return {
    dashboard,
    loading,
    fetchDashboard,
  }
})

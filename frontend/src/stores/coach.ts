import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  completeCoachTask as completeCoachTaskApi,
  generateCoachPlan as generateCoachPlanApi,
  getCoachDiagnosis as getCoachDiagnosisApi,
  getCoachOverview as getCoachOverviewApi,
  getCoachPlan as getCoachPlanApi,
  listCoachDiagnoses as listCoachDiagnosesApi,
  listCoachPlans as listCoachPlansApi,
  reopenCoachTask as reopenCoachTaskApi,
} from '@/api/coach'
import type {
  CoachDiagnosisVO,
  CoachGenerateRequest,
  CoachOverviewVO,
  CoachPlanVO,
} from '@/types/coach'

export const useCoachStore = defineStore('coach', () => {
  const overview = ref<CoachOverviewVO | null>(null)
  const diagnoses = ref<CoachDiagnosisVO[]>([])
  const plans = ref<CoachPlanVO[]>([])
  const currentDiagnosis = ref<CoachDiagnosisVO | null>(null)
  const currentPlan = ref<CoachPlanVO | null>(null)
  const loading = ref(false)
  const generating = ref(false)

  async function generate(data: CoachGenerateRequest) {
    generating.value = true
    try {
      const res = await generateCoachPlanApi(data)
      if (res.code === 0) {
        overview.value = {
          latestDiagnosis: res.data.diagnosis,
          activePlan: res.data.plan,
          todayTasks: res.data.plan.tasks.filter((task) => task.dayIndex === 1),
          diagnosisCount: (overview.value?.diagnosisCount || 0) + 1,
          planCount: (overview.value?.planCount || 0) + 1,
        }
        return res.data
      }
      return null
    } finally {
      generating.value = false
    }
  }

  async function fetchOverview() {
    loading.value = true
    try {
      const res = await getCoachOverviewApi()
      if (res.code === 0) overview.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchDiagnoses() {
    const res = await listCoachDiagnosesApi()
    if (res.code === 0) diagnoses.value = res.data
  }

  async function fetchDiagnosis(id: number) {
    loading.value = true
    try {
      const res = await getCoachDiagnosisApi(id)
      if (res.code === 0) currentDiagnosis.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchPlans() {
    const res = await listCoachPlansApi()
    if (res.code === 0) plans.value = res.data
  }

  async function fetchPlan(id: number) {
    loading.value = true
    try {
      const res = await getCoachPlanApi(id)
      if (res.code === 0) currentPlan.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function completeTask(id: number) {
    const res = await completeCoachTaskApi(id)
    if (res.code !== 0) return false
    if (currentPlan.value) {
      const task = currentPlan.value.tasks.find((item) => item.id === id)
      if (task) task.status = 'completed'
      currentPlan.value.completedTaskCount = currentPlan.value.tasks.filter((item) => item.status === 'completed').length
    }
    return true
  }

  async function reopenTask(id: number) {
    const res = await reopenCoachTaskApi(id)
    if (res.code !== 0) return false
    if (currentPlan.value) {
      const task = currentPlan.value.tasks.find((item) => item.id === id)
      if (task) task.status = 'pending'
      currentPlan.value.completedTaskCount = currentPlan.value.tasks.filter((item) => item.status === 'completed').length
    }
    return true
  }

  return {
    overview,
    diagnoses,
    plans,
    currentDiagnosis,
    currentPlan,
    loading,
    generating,
    generate,
    fetchOverview,
    fetchDiagnoses,
    fetchDiagnosis,
    fetchPlans,
    fetchPlan,
    completeTask,
    reopenTask,
  }
})

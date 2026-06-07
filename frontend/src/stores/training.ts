import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  generateTrainingPlan as generateTrainingPlanApi,
  getActiveTrainingPlan as getActiveTrainingPlanApi,
  listTrainingPlans as listTrainingPlansApi,
  getTrainingPlan as getTrainingPlanApi,
  archiveTrainingPlan as archiveTrainingPlanApi,
  completeTrainingPlan as completeTrainingPlanApi,
  getTrainingQuestion as getTrainingQuestionApi,
  masterTrainingQuestion as masterTrainingQuestionApi,
  skipTrainingQuestion as skipTrainingQuestionApi,
  submitTrainingAnswer as submitTrainingAnswerApi,
  listTrainingQuestionAnswers as listTrainingQuestionAnswersApi,
  completeAlgorithmRecommendation as completeAlgorithmRecommendationApi,
  reopenAlgorithmRecommendation as reopenAlgorithmRecommendationApi,
} from '@/api/training'
import type {
  TrainingAnswerSubmitRequest,
  TrainingAnswerVO,
  TrainingPlanGenerateRequest,
  TrainingPlanVO,
  TrainingQuestionVO,
} from '@/types/training'

export const useTrainingStore = defineStore('training', () => {
  const plans = ref<TrainingPlanVO[]>([])
  const activePlan = ref<TrainingPlanVO | null>(null)
  const currentPlan = ref<TrainingPlanVO | null>(null)
  const currentQuestion = ref<TrainingQuestionVO | null>(null)
  const answers = ref<TrainingAnswerVO[]>([])
  const loading = ref(false)

  async function generatePlan(data: TrainingPlanGenerateRequest) {
    loading.value = true
    try {
      const res = await generateTrainingPlanApi(data)
      if (res.data.code === 0) {
        return res.data.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchActivePlan() {
    loading.value = true
    try {
      const res = await getActiveTrainingPlanApi()
      if (res.data.code === 0) {
        activePlan.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchPlans() {
    loading.value = true
    try {
      const res = await listTrainingPlansApi()
      if (res.data.code === 0) {
        plans.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchPlan(id: number) {
    loading.value = true
    try {
      const res = await getTrainingPlanApi(id)
      if (res.data.code === 0) {
        currentPlan.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function archivePlan(id: number) {
    const res = await archiveTrainingPlanApi(id)
    return res.data.code === 0
  }

  async function completePlan(id: number) {
    const res = await completeTrainingPlanApi(id)
    return res.data.code === 0
  }

  async function fetchQuestion(id: number) {
    loading.value = true
    try {
      const res = await getTrainingQuestionApi(id)
      if (res.data.code === 0) {
        currentQuestion.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function masterQuestion(id: number) {
    const res = await masterTrainingQuestionApi(id)
    return res.data.code === 0
  }

  async function skipQuestion(id: number) {
    const res = await skipTrainingQuestionApi(id)
    return res.data.code === 0
  }

  async function submitAnswer(id: number, data: TrainingAnswerSubmitRequest) {
    loading.value = true
    try {
      const res = await submitTrainingAnswerApi(id, data)
      if (res.data.code === 0) {
        return res.data.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchAnswers(id: number) {
    const res = await listTrainingQuestionAnswersApi(id)
    if (res.data.code === 0) {
      answers.value = res.data.data
    }
  }

  async function completeAlgorithm(id: number) {
    const res = await completeAlgorithmRecommendationApi(id)
    return res.data.code === 0
  }

  async function reopenAlgorithm(id: number) {
    const res = await reopenAlgorithmRecommendationApi(id)
    return res.data.code === 0
  }

  return {
    plans,
    activePlan,
    currentPlan,
    currentQuestion,
    answers,
    loading,
    generatePlan,
    fetchActivePlan,
    fetchPlans,
    fetchPlan,
    archivePlan,
    completePlan,
    fetchQuestion,
    masterQuestion,
    skipQuestion,
    submitAnswer,
    fetchAnswers,
    completeAlgorithm,
    reopenAlgorithm,
  }
})

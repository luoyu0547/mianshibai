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
  listTrainingMistakes as listTrainingMistakesApi,
  listTopicMastery as listTopicMasteryApi,
  listSkillTagMastery as listSkillTagMasteryApi,
  getTrainingMasterySummary as getTrainingMasterySummaryApi,
  rebuildTrainingMastery as rebuildTrainingMasteryApi,
} from '@/api/training'
import type {
  TrainingAnswerSubmitRequest,
  TrainingAnswerVO,
  TrainingMistakeQueryRequest,
  TrainingMistakeVO,
  TrainingMasteryVO,
  TrainingMasterySummaryVO,
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
  const mistakes = ref<TrainingMistakeVO[]>([])
  const topicMasteries = ref<TrainingMasteryVO[]>([])
  const skillTagMasteries = ref<TrainingMasteryVO[]>([])
  const masterySummary = ref<TrainingMasterySummaryVO | null>(null)
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

  async function fetchMistakes(params?: TrainingMistakeQueryRequest) {
    loading.value = true
    try {
      const res = await listTrainingMistakesApi(params)
      if (res.data.code === 0) {
        mistakes.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchTopicMastery() {
    loading.value = true
    try {
      const res = await listTopicMasteryApi()
      if (res.data.code === 0) {
        topicMasteries.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchSkillTagMastery() {
    loading.value = true
    try {
      const res = await listSkillTagMasteryApi()
      if (res.data.code === 0) {
        skillTagMasteries.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchMasterySummary() {
    const res = await getTrainingMasterySummaryApi()
    if (res.data.code === 0) {
      masterySummary.value = res.data.data
    }
  }

  async function rebuildMastery() {
    loading.value = true
    try {
      const res = await rebuildTrainingMasteryApi()
      if (res.data.code === 0) {
        return true
      }
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    plans,
    activePlan,
    currentPlan,
    currentQuestion,
    answers,
    mistakes,
    topicMasteries,
    skillTagMasteries,
    masterySummary,
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
    fetchMistakes,
    fetchTopicMastery,
    fetchSkillTagMastery,
    fetchMasterySummary,
    rebuildMastery,
  }
})

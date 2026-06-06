// src/stores/interview.ts
import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  listInterviewSessions as listApi,
  getInterviewSession as getDetailApi,
  getInterviewReport as getReportApi,
  createInterviewSession as createApi,
  startInterviewSession as startApi,
  submitInterviewAnswer as submitApi,
  cancelInterviewSession as cancelApi,
} from '@/api/interview'
import type {
  InterviewSessionVO,
  InterviewQuestionVO,
  InterviewReportVO,
  InterviewCreateRequest,
  InterviewAnswerRequest,
} from '@/types/interview'

export const useInterviewStore = defineStore('interview', () => {
  const sessions = ref<InterviewSessionVO[]>([])
  const currentSession = ref<InterviewSessionVO | null>(null)
  const currentQuestion = ref<InterviewQuestionVO | null>(null)
  const currentReport = ref<InterviewReportVO | null>(null)
  const loading = ref(false)

  async function fetchSessions() {
    loading.value = true
    try {
      const res = await listApi()
      if (res.data.code === 0) {
        sessions.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchSession(id: number) {
    loading.value = true
    try {
      const res = await getDetailApi(id)
      if (res.data.code === 0) {
        currentSession.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createSession(data: InterviewCreateRequest) {
    const res = await createApi(data)
    return res
  }

  async function startSession(sessionId: number) {
    loading.value = true
    try {
      const res = await startApi(sessionId)
      if (res.data.code === 0) {
        currentQuestion.value = res.data.data
      }
      return res
    } finally {
      loading.value = false
    }
  }

  async function submitAnswer(sessionId: number, turnId: number, data: InterviewAnswerRequest) {
    loading.value = true
    try {
      const res = await submitApi(sessionId, turnId, data)
      return res
    } finally {
      loading.value = false
    }
  }

  async function fetchReport(sessionId: number) {
    loading.value = true
    try {
      const res = await getReportApi(sessionId)
      if (res.data.code === 0) {
        currentReport.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function cancelSession(sessionId: number) {
    const res = await cancelApi(sessionId)
    if (res.data.code === 0) {
      sessions.value = sessions.value.filter((s) => s.id !== sessionId)
      return true
    }
    return false
  }

  return {
    sessions,
    currentSession,
    currentQuestion,
    currentReport,
    loading,
    fetchSessions,
    fetchSession,
    createSession,
    startSession,
    submitAnswer,
    fetchReport,
    cancelSession,
  }
})

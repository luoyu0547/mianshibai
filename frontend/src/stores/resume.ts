// src/stores/resume.ts
import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  listResumes as listResumesApi,
  getResumeDetail as getResumeDetailApi,
  createResume as createResumeApi,
  deleteResume as deleteResumeApi,
  aiGenerateResume as aiGenerateResumeApi,
} from '@/api/resume'
import type {
  ResumeVO,
  ResumeDetailVO,
  ResumeCreateRequest,
  AiGenerateRequest,
} from '@/types/resume'

export const useResumeStore = defineStore('resume', () => {
  const resumeList = ref<ResumeVO[]>([])
  const currentResume = ref<ResumeDetailVO | null>(null)
  const loading = ref(false)

  async function fetchResumeList() {
    loading.value = true
    try {
      const res = await listResumesApi()
      if (res.data.code === 0) {
        resumeList.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchResumeDetail(id: number) {
    loading.value = true
    try {
      const res = await getResumeDetailApi(id)
      if (res.data.code === 0) {
        currentResume.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createResume(data: ResumeCreateRequest) {
    const res = await createResumeApi(data)
    return res
  }

  async function deleteResume(id: number) {
    const res = await deleteResumeApi(id)
    if (res.data.code === 0) {
      resumeList.value = resumeList.value.filter((r) => r.id !== id)
      return true
    }
    return false
  }

  async function aiGenerateResume(data: AiGenerateRequest) {
    loading.value = true
    try {
      const res = await aiGenerateResumeApi(data)
      if (res.data.code === 0) {
        currentResume.value = res.data.data
      }
      return res
    } finally {
      loading.value = false
    }
  }

  return {
    resumeList,
    currentResume,
    loading,
    fetchResumeList,
    fetchResumeDetail,
    createResume,
    deleteResume,
    aiGenerateResume,
  }
})

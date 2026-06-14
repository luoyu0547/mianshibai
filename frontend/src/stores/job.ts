// src/stores/job.ts
import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  importJobUrl as importJobUrlApi,
  getJobDetail as getJobDetailApi,
  getCompanyDetail as getCompanyDetailApi,
  matchJob as matchJobApi,
  favoriteJob as favoriteJobApi,
  unfavoriteJob as unfavoriteJobApi,
  listFavoriteJobs as listFavoriteJobsApi,
} from '@/api/job'
import type {
  JobImportRequest,
  JobVO,
  CompanyVO,
  JobMatchVO,
  JobMatchRequest,
} from '@/types/job'

export const useJobStore = defineStore('job', () => {
  const currentJob = ref<JobVO | null>(null)
  const currentCompany = ref<CompanyVO | null>(null)
  const currentMatch = ref<JobMatchVO | null>(null)
  const favoriteList = ref<JobVO[]>([])
  const loading = ref(false)

  async function importUrl(data: JobImportRequest) {
    loading.value = true
    try {
      const res = await importJobUrlApi(data)
      if (res.code === 0) {
        return res.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchJobDetail(jobId: number) {
    loading.value = true
    try {
      const res = await getJobDetailApi(jobId)
      if (res.code === 0) {
        currentJob.value = res.data
        currentMatch.value = res.data.matchResult
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchCompanyDetail(companyId: number) {
    loading.value = true
    try {
      const res = await getCompanyDetailApi(companyId)
      if (res.code === 0) {
        currentCompany.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  async function matchResume(jobId: number, data: JobMatchRequest) {
    loading.value = true
    try {
      const res = await matchJobApi(jobId, data)
      if (res.code === 0) {
        currentMatch.value = res.data
        return res.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function toggleFavorite(jobId: number) {
    if (!currentJob.value) return
    try {
      if (currentJob.value.favorited) {
        const res = await unfavoriteJobApi(jobId)
        if (res.code === 0) {
          currentJob.value.favorited = false
        }
      } else {
        const res = await favoriteJobApi(jobId)
        if (res.code === 0) {
          currentJob.value.favorited = true
        }
      }
    } catch {
      // ignore
    }
  }

  async function fetchFavoriteList() {
    loading.value = true
    try {
      const res = await listFavoriteJobsApi()
      if (res.code === 0) {
        favoriteList.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  return {
    currentJob,
    currentCompany,
    currentMatch,
    favoriteList,
    loading,
    importUrl,
    fetchJobDetail,
    fetchCompanyDetail,
    matchResume,
    toggleFavorite,
    fetchFavoriteList,
  }
})

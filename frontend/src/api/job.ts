// src/api/job.ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { CompanyVO, JobImportRequest, JobImportResultVO, JobListQueryRequest, JobMatchRequest, JobMatchVO, JobKeywordVO, JobGapAnalysisVO, JobQuestionPredictionVO, JobVO } from '@/types/job'

export function importJobUrl(data: JobImportRequest) {
  return request.post<BaseResponse<JobImportResultVO>>('/api/job/import-url', data)
}

export function getJobDetail(jobId: number) {
  return request.get<BaseResponse<JobVO>>(`/api/job/${jobId}`)
}

export function getCompanyDetail(companyId: number) {
  return request.get<BaseResponse<CompanyVO>>(`/api/job/company/${companyId}`)
}

export function matchJob(jobId: number, data: JobMatchRequest) {
  return request.post<BaseResponse<JobMatchVO>>(`/api/job/${jobId}/match`, data)
}

export function favoriteJob(jobId: number) {
  return request.post<BaseResponse<boolean>>(`/api/job/${jobId}/favorite`)
}

export function unfavoriteJob(jobId: number) {
  return request.delete<BaseResponse<boolean>>(`/api/job/${jobId}/favorite`)
}

export function listFavoriteJobs() {
  return request.get<BaseResponse<JobVO[]>>('/api/job/favorites')
}

export function listJobs(params: JobListQueryRequest) {
  return request.get<BaseResponse<JobVO[]>>('/api/job/list', { params })
}

export function extractJobKeywords(jobId: number) {
  return request.post<BaseResponse<JobKeywordVO>>(`/api/job/${jobId}/keywords`)
}

export function analyzeJobGap(jobId: number, resumeId: number) {
  return request.post<BaseResponse<JobGapAnalysisVO>>(`/api/job/${jobId}/gap`, null, { params: { resumeId } })
}

export function predictJobQuestions(jobId: number) {
  return request.post<BaseResponse<JobQuestionPredictionVO>>(`/api/job/${jobId}/questions`)
}

// src/api/job.ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { CompanyVO, JobImportRequest, JobImportResultVO, JobMatchRequest, JobMatchVO, JobVO } from '@/types/job'

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

import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  AdminJobCrawlTaskVO,
  AdminJobCrawlTaskCreateRequest,
  AdminJobCrawlTaskUpdateRequest,
  AdminJobCrawlTaskQueryRequest,
  AdminJobCrawlRunVO,
  AdminJobCrawlItemVO,
} from '@/types/jobCrawl'

export function listJobCrawlTasks(params?: AdminJobCrawlTaskQueryRequest) {
  return request.get<BaseResponse<AdminJobCrawlTaskVO[]>>('/api/admin/job-crawl/tasks', { params })
}

export function getJobCrawlTask(id: number) {
  return request.get<BaseResponse<AdminJobCrawlTaskVO>>(`/api/admin/job-crawl/tasks/${id}`)
}

export function createJobCrawlTask(data: AdminJobCrawlTaskCreateRequest) {
  return request.post<BaseResponse<AdminJobCrawlTaskVO>>('/api/admin/job-crawl/tasks', data)
}

export function updateJobCrawlTask(id: number, data: AdminJobCrawlTaskUpdateRequest) {
  return request.put<BaseResponse<AdminJobCrawlTaskVO>>(`/api/admin/job-crawl/tasks/${id}`, data)
}

export function enableJobCrawlTask(id: number) {
  return request.put<BaseResponse<AdminJobCrawlTaskVO>>(`/api/admin/job-crawl/tasks/${id}/enable`)
}

export function disableJobCrawlTask(id: number) {
  return request.put<BaseResponse<AdminJobCrawlTaskVO>>(`/api/admin/job-crawl/tasks/${id}/disable`)
}

export function runJobCrawlTask(id: number) {
  return request.post<BaseResponse<AdminJobCrawlRunVO>>(`/api/admin/job-crawl/tasks/${id}/run`)
}

export function listJobCrawlTaskRuns(taskId: number) {
  return request.get<BaseResponse<AdminJobCrawlRunVO[]>>(`/api/admin/job-crawl/tasks/${taskId}/runs`)
}

export function listJobCrawlRunItems(runId: number) {
  return request.get<BaseResponse<AdminJobCrawlItemVO[]>>(`/api/admin/job-crawl/runs/${runId}/items`)
}

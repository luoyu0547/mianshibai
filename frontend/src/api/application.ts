import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  ApplicationCreateRequest,
  ApplicationListQueryRequest,
  ApplicationStatsVO,
  ApplicationStatus,
  ApplicationTodoCreateRequest,
  ApplicationTodoQueryRequest,
  ApplicationTodoUpdateRequest,
  ApplicationTodoVO,
  ApplicationUpdateRequest,
  JobApplicationVO,
} from '@/types/application'

export interface ApplicationStatusUpdateRequest {
  status: ApplicationStatus
}

export function createApplication(data: ApplicationCreateRequest) {
  return request.post<BaseResponse<JobApplicationVO>>('/api/application', data)
}

export function listApplications(params?: ApplicationListQueryRequest) {
  return request.get<BaseResponse<JobApplicationVO[]>>('/api/application', { params })
}

export function getApplicationStats() {
  return request.get<BaseResponse<ApplicationStatsVO>>('/api/application/stats')
}

export function getApplication(id: number) {
  return request.get<BaseResponse<JobApplicationVO>>(`/api/application/${id}`)
}

export function updateApplication(id: number, data: ApplicationUpdateRequest) {
  return request.put<BaseResponse<JobApplicationVO>>(`/api/application/${id}`, data)
}

export function updateApplicationStatus(id: number, data: ApplicationStatusUpdateRequest) {
  return request.put<BaseResponse<JobApplicationVO>>(`/api/application/${id}/status`, data)
}

export function deleteApplication(id: number) {
  return request.delete<BaseResponse<boolean>>(`/api/application/${id}`)
}

export function createApplicationTodo(applicationId: number, data: ApplicationTodoCreateRequest) {
  return request.post<BaseResponse<ApplicationTodoVO>>(`/api/application/${applicationId}/todo`, data)
}

export function createGlobalTodo(data: ApplicationTodoCreateRequest) {
  return request.post<BaseResponse<ApplicationTodoVO>>('/api/application/todo', data)
}

export function listApplicationTodos(params?: ApplicationTodoQueryRequest) {
  return request.get<BaseResponse<ApplicationTodoVO[]>>('/api/application/todo', { params })
}

export function updateApplicationTodo(id: number, data: ApplicationTodoUpdateRequest) {
  return request.put<BaseResponse<ApplicationTodoVO>>(`/api/application/todo/${id}`, data)
}

export function completeApplicationTodo(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/application/todo/${id}/complete`)
}

export function reopenApplicationTodo(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/application/todo/${id}/reopen`)
}

export function deleteApplicationTodo(id: number) {
  return request.delete<BaseResponse<boolean>>(`/api/application/todo/${id}`)
}

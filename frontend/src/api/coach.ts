import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  CoachDiagnosisVO,
  CoachGenerateRequest,
  CoachGenerateResultVO,
  CoachOverviewVO,
  CoachPlanVO,
  CoachTaskVO,
} from '@/types/coach'

export function generateCoachPlan(data: CoachGenerateRequest) {
  return request.post<BaseResponse<CoachGenerateResultVO>>('/api/coach/generate', data)
}

export function getCoachOverview() {
  return request.get<BaseResponse<CoachOverviewVO>>('/api/coach/overview')
}

export function listCoachDiagnoses() {
  return request.get<BaseResponse<CoachDiagnosisVO[]>>('/api/coach/diagnoses')
}

export function getCoachDiagnosis(id: number) {
  return request.get<BaseResponse<CoachDiagnosisVO>>(`/api/coach/diagnoses/${id}`)
}

export function listCoachPlans() {
  return request.get<BaseResponse<CoachPlanVO[]>>('/api/coach/plans')
}

export function getCoachPlan(id: number) {
  return request.get<BaseResponse<CoachPlanVO>>(`/api/coach/plans/${id}`)
}

export function completeCoachTask(id: number) {
  return request.put<BaseResponse<CoachTaskVO>>(`/api/coach/tasks/${id}/complete`)
}

export function reopenCoachTask(id: number) {
  return request.put<BaseResponse<CoachTaskVO>>(`/api/coach/tasks/${id}/reopen`)
}

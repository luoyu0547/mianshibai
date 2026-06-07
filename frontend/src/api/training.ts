import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  TrainingAnswerSubmitRequest,
  TrainingAnswerVO,
  TrainingPlanGenerateRequest,
  TrainingPlanVO,
  TrainingQuestionVO,
} from '@/types/training'

export function generateTrainingPlan(data: TrainingPlanGenerateRequest) {
  return request.post<BaseResponse<TrainingPlanVO>>('/api/training/plan/generate', data)
}

export function getActiveTrainingPlan() {
  return request.get<BaseResponse<TrainingPlanVO | null>>('/api/training/plan/active')
}

export function listTrainingPlans() {
  return request.get<BaseResponse<TrainingPlanVO[]>>('/api/training/plan')
}

export function getTrainingPlan(id: number) {
  return request.get<BaseResponse<TrainingPlanVO>>(`/api/training/plan/${id}`)
}

export function archiveTrainingPlan(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/training/plan/${id}/archive`)
}

export function completeTrainingPlan(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/training/plan/${id}/complete`)
}

export function getTrainingQuestion(id: number) {
  return request.get<BaseResponse<TrainingQuestionVO>>(`/api/training/question/${id}`)
}

export function masterTrainingQuestion(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/training/question/${id}/master`)
}

export function skipTrainingQuestion(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/training/question/${id}/skip`)
}

export function submitTrainingAnswer(id: number, data: TrainingAnswerSubmitRequest) {
  return request.post<BaseResponse<TrainingAnswerVO>>(`/api/training/question/${id}/answer`, data)
}

export function listTrainingQuestionAnswers(id: number) {
  return request.get<BaseResponse<TrainingAnswerVO[]>>(`/api/training/question/${id}/answers`)
}

export function completeAlgorithmRecommendation(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/training/algorithm/${id}/complete`)
}

export function reopenAlgorithmRecommendation(id: number) {
  return request.put<BaseResponse<boolean>>(`/api/training/algorithm/${id}/reopen`)
}

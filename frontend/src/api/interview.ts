// src/api/interview.ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  InterviewCreateRequest,
  InterviewAnswerRequest,
  InterviewSessionVO,
  InterviewQuestionVO,
  InterviewAnswerResultVO,
  InterviewReportVO,
} from '@/types/interview'

export function createInterviewSession(data: InterviewCreateRequest) {
  return request.post<BaseResponse<InterviewSessionVO>>('/api/interview/session', data)
}

export function startInterviewSession(sessionId: number) {
  return request.post<BaseResponse<InterviewQuestionVO>>(`/api/interview/session/${sessionId}/start`)
}

export function submitInterviewAnswer(sessionId: number, turnId: number, data: InterviewAnswerRequest) {
  return request.post<BaseResponse<InterviewAnswerResultVO>>(`/api/interview/session/${sessionId}/turn/${turnId}/answer`, data)
}

export function getInterviewSession(sessionId: number) {
  return request.get<BaseResponse<InterviewSessionVO>>(`/api/interview/session/${sessionId}`)
}

export function listInterviewSessions() {
  return request.get<BaseResponse<InterviewSessionVO[]>>('/api/interview/session/list')
}

export function getInterviewReport(sessionId: number) {
  return request.get<BaseResponse<InterviewReportVO>>(`/api/interview/session/${sessionId}/report`)
}

export function cancelInterviewSession(sessionId: number) {
  return request.post<BaseResponse<null>>(`/api/interview/session/${sessionId}/cancel`)
}

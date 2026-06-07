// src/api/resume.ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  ResumeCreateRequest,
  ResumeUpdateRequest,
  ResumeVO,
  ResumeDetailVO,
  SectionCreateRequest,
  SectionUpdateRequest,
  SectionSortRequest,
  SectionVO,
  AiGenerateRequest,
  AiOptimizeRequest,
  AiScoreVO,
  VersionVO,
  ChatMessageVO,
} from '@/types/resume'

export function createResume(data: ResumeCreateRequest) {
  return request.post<BaseResponse<ResumeVO>>('/api/resume', data)
}

export function listResumes() {
  return request.get<BaseResponse<ResumeVO[]>>('/api/resume/list')
}

export function getResumeDetail(id: number) {
  return request.get<BaseResponse<ResumeDetailVO>>(`/api/resume/${id}`)
}

export function updateResume(id: number, data: ResumeUpdateRequest) {
  return request.put<BaseResponse<ResumeVO>>(`/api/resume/${id}`, data)
}

export function deleteResume(id: number) {
  return request.delete<BaseResponse<boolean>>(`/api/resume/${id}`)
}

export function addSection(resumeId: number, data: SectionCreateRequest) {
  return request.post<BaseResponse<SectionVO>>(`/api/resume/${resumeId}/section`, data)
}

export function updateSection(resumeId: number, sectionId: number, data: SectionUpdateRequest) {
  return request.put<BaseResponse<SectionVO>>(`/api/resume/${resumeId}/section/${sectionId}`, data)
}

export function deleteSection(resumeId: number, sectionId: number) {
  return request.delete<BaseResponse<boolean>>(`/api/resume/${resumeId}/section/${sectionId}`)
}

export function sortSections(resumeId: number, data: SectionSortRequest) {
  return request.put<BaseResponse<boolean>>(`/api/resume/${resumeId}/section/sort`, data)
}

export function aiGenerateResume(data: AiGenerateRequest) {
  return request.post<BaseResponse<ResumeDetailVO>>('/api/resume/ai/generate', data)
}

export function aiOptimizeSection(resumeId: number, data: AiOptimizeRequest) {
  return request.post<BaseResponse<SectionVO>>(`/api/resume/${resumeId}/ai/optimize-section`, data)
}

export function aiScoreResume(resumeId: number) {
  return request.post<BaseResponse<AiScoreVO>>(`/api/resume/${resumeId}/ai/score`)
}

export function getResumeVersions(id: number) {
  return request.get<BaseResponse<VersionVO[]>>(`/api/resume/${id}/versions`)
}

export function getChatHistory(resumeId: number) {
  return request.get<BaseResponse<ChatMessageVO[]>>(`/api/resume/${resumeId}/chat/history`)
}

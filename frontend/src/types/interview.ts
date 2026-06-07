// src/types/interview.ts

export type InterviewStatus = 'created' | 'in_progress' | 'generating_report' | 'completed' | 'cancelled'
export type InterviewTurnType = 'main' | 'follow_up'
export type InterviewNextAction = 'FOLLOW_UP' | 'NEXT_QUESTION' | 'REPORT_READY'

export interface InterviewSessionVO {
  id: number
  resumeId: number
  title: string
  interviewType: string
  targetPosition: string
  techDirection: string
  totalQuestions: number
  currentQuestionNo: number
  status: InterviewStatus
  difficulty: string
  durationMinutes: number | null
  jobId: number | null
  startedAt: string | null
  endedAt: string | null
  createTime: string
  updateTime: string
}

export interface InterviewTurnVO {
  id: number
  sessionId: number
  questionNo: number
  turnType: InterviewTurnType
  questionText: string
  answerText: string | null
  aiFeedback: string | null
  answerDurationSeconds: number | null
  createTime: string
  updateTime: string
}

export interface InterviewQuestionVO {
  turnId: number
  questionNo: number
  turnType: InterviewTurnType
  questionText: string
  ttsAudioBase64: string
}

export interface InterviewAnswerResultVO {
  nextAction: InterviewNextAction
  turn: InterviewQuestionVO | null
  reportId: number | null
}

export interface InterviewReportVO {
  id: number
  sessionId: number
  totalScore: number
  accuracyScore: number
  clarityScore: number
  depthScore: number
  matchingScore: number
  summary: string
  suggestions: string[]
  turns: InterviewTurnVO[]
  createTime: string
}

export interface InterviewCreateRequest {
  resumeId: number
  targetPosition: string
  techDirection?: string
  jobId?: number
  interviewType?: 'technical' | 'project' | 'hr' | 'system_design'
  difficulty?: 'easy' | 'medium' | 'hard'
  durationMinutes?: 10 | 20 | 30 | 45 | 60
}

export interface InterviewAnswerRequest {
  answerText: string
  answerDurationSeconds?: number
}

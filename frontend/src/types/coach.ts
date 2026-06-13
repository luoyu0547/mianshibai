export type CoachSource = 'ai' | 'fallback'
export type CoachPlanStatus = 'active' | 'completed' | 'archived'
export type CoachTaskStatus = 'pending' | 'completed'
export type CoachTaskType = 'resume' | 'interview' | 'training' | 'application' | 'job' | 'habit'
export type CoachTaskPriority = 'high' | 'medium' | 'low'

export interface CoachGenerateRequest {
  targetPosition?: string
  focus?: string
}

export interface CoachTaskVO {
  id: number
  planId: number
  dayIndex: number
  title: string
  description: string
  taskType: CoachTaskType
  priority: CoachTaskPriority
  status: CoachTaskStatus
  referenceType?: string | null
  referenceId?: number | null
  completedAt?: string | null
  createTime: string
  updateTime: string
}

export interface CoachDiagnosisVO {
  id: number
  title: string
  overallScore: number
  summary: string
  strengths: string[]
  weaknesses: string[]
  suggestions: string[]
  dataSnapshot: Record<string, unknown>
  dataCompleteness: number
  source: CoachSource
  createTime: string
  updateTime: string
}

export interface CoachPlanVO {
  id: number
  diagnosisId: number
  title: string
  summary: string
  targetPosition: string
  targetDays: number
  status: CoachPlanStatus
  source: CoachSource
  totalTaskCount: number
  completedTaskCount: number
  tasks: CoachTaskVO[]
  createTime: string
  updateTime: string
}

export interface CoachOverviewVO {
  latestDiagnosis: CoachDiagnosisVO | null
  activePlan: CoachPlanVO | null
  todayTasks: CoachTaskVO[]
  diagnosisCount: number
  planCount: number
}

export interface CoachGenerateResultVO {
  diagnosis: CoachDiagnosisVO
  plan: CoachPlanVO
}

export const COACH_PLAN_STATUS_LABELS: Record<CoachPlanStatus, string> = {
  active: '进行中',
  completed: '已完成',
  archived: '已归档',
}

export const COACH_TASK_TYPE_LABELS: Record<CoachTaskType, string> = {
  resume: '简历优化',
  interview: '面试训练',
  training: '八股训练',
  application: '投递跟进',
  job: '职位分析',
  habit: '求职习惯',
}

export const COACH_PRIORITY_LABELS: Record<CoachTaskPriority, string> = {
  high: '高',
  medium: '中',
  low: '低',
}

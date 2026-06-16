export type ApplicationStatus =
  | 'pending_submit'
  | 'submitted'
  | 'interviewing'
  | 'offer'
  | 'rejected'
  | 'withdrawn'

export type TodoPriority = 'low' | 'medium' | 'high'

export interface ApplicationTodoVO {
  id: number
  applicationId?: number | null
  applicationCompanyName?: string
  applicationJobTitle?: string
  title: string
  description?: string
  priority: TodoPriority
  priorityLabel: string
  dueAt?: string | null
  completed: boolean
  completedAt?: string | null
  createTime: string
  updateTime: string
}

export interface JobApplicationVO {
  id: number
  resumeId?: number | null
  companyName: string
  jobTitle: string
  source: string
  status: ApplicationStatus
  statusLabel: string
  appliedAt?: string | null
  nextEventAt?: string | null
  salaryRange: string
  location: string
  contactName: string
  contactInfo: string
  notes?: string
  unfinishedTodoCount: number
  todos?: ApplicationTodoVO[]
  rounds: ApplicationRoundVO[]
  createTime: string
  updateTime: string
}

export interface ApplicationStatsVO {
  total: number
  pendingSubmit: number
  submitted: number
  interviewing: number
  offer: number
  closed: number
}

export interface ApplicationCreateRequest {
  resumeId?: number | null
  companyName: string
  jobTitle: string
  source?: string
  status?: ApplicationStatus
  appliedAt?: string | null
  nextEventAt?: string | null
  salaryRange?: string
  location?: string
  contactName?: string
  contactInfo?: string
  notes?: string
}

export type ApplicationUpdateRequest = Partial<ApplicationCreateRequest>

export interface ApplicationListQueryRequest {
  keyword?: string
  status?: ApplicationStatus | ''
  location?: string
  source?: string
  resumeId?: number
}

export interface ApplicationTodoCreateRequest {
  title: string
  description?: string
  priority?: TodoPriority
  dueAt?: string | null
}

export type ApplicationTodoUpdateRequest = Partial<ApplicationTodoCreateRequest>

export interface ApplicationTodoQueryRequest {
  applicationId?: number
  completed?: boolean
  priority?: TodoPriority | ''
  overdue?: boolean
}

export type RoundResult = 'pending' | 'pass' | 'fail'

export interface ApplicationRoundVO {
  id: number
  applicationId: number
  roundName: string
  roundOrder: number
  scheduledAt: string | null
  result: RoundResult
  notes: string | null
}

export interface ApplicationRoundCreateRequest {
  roundName: string
  scheduledAt?: string | null
  notes?: string
}

export interface ApplicationRoundUpdateRequest {
  roundName?: string
  scheduledAt?: string | null
  notes?: string
}

export interface ApplicationRoundResultRequest {
  result: RoundResult
}

export const APPLICATION_STATUS_OPTIONS: Array<{ label: string; value: ApplicationStatus }> = [
  { label: '待投递', value: 'pending_submit' },
  { label: '已投递', value: 'submitted' },
  { label: '面试中', value: 'interviewing' },
  { label: 'Offer', value: 'offer' },
  { label: '拒绝', value: 'rejected' },
  { label: '放弃', value: 'withdrawn' },
]

export type ApplicationSource = 'boss' | 'lagou' | 'liepin' | 'zhilian' | '51job' | 'internal_referral' | 'official_website' | 'nowcoder' | 'maimai' | 'campus_recruitment' | 'other'

export const APPLICATION_SOURCE_OPTIONS: Array<{ label: string; value: ApplicationSource }> = [
  { label: 'Boss 直聘', value: 'boss' },
  { label: '拉勾', value: 'lagou' },
  { label: '猎聘', value: 'liepin' },
  { label: '智联招聘', value: 'zhilian' },
  { label: '前程无忧', value: '51job' },
  { label: '内推', value: 'internal_referral' },
  { label: '公司官网', value: 'official_website' },
  { label: '牛客网', value: 'nowcoder' },
  { label: '脉脉', value: 'maimai' },
  { label: '校园招聘', value: 'campus_recruitment' },
  { label: '其他', value: 'other' },
]

export const TODO_PRIORITY_OPTIONS: Array<{ label: string; value: TodoPriority }> = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
]

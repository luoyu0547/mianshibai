export type ApplicationStatus =
  | 'pending_submit'
  | 'submitted'
  | 'hr_contact'
  | 'written_test'
  | 'first_interview'
  | 'second_interview'
  | 'final_interview'
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
  jobId?: number | null
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
  jobId?: number | null
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
  jobId?: number
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

export const APPLICATION_STATUS_OPTIONS: Array<{ label: string; value: ApplicationStatus }> = [
  { label: '待投递', value: 'pending_submit' },
  { label: '已投递', value: 'submitted' },
  { label: 'HR 沟通', value: 'hr_contact' },
  { label: '笔试', value: 'written_test' },
  { label: '一面', value: 'first_interview' },
  { label: '二面', value: 'second_interview' },
  { label: '终面', value: 'final_interview' },
  { label: 'Offer', value: 'offer' },
  { label: '拒绝', value: 'rejected' },
  { label: '放弃', value: 'withdrawn' },
]

export const TODO_PRIORITY_OPTIONS: Array<{ label: string; value: TodoPriority }> = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
]

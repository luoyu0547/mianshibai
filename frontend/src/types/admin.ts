export interface AdminOverviewVO {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  adminUsers: number
  resumeCount: number
  interviewCount: number
  completedInterviewCount: number
  applicationCount: number
  trainingPlanCount: number
  trainingAnswerCount: number
  trainingReviewCount: number
}

export interface AdminUserListItemVO {
  id: number
  userAccount: string
  userName: string
  userAvatar: string
  userRole: string
  userStatus: number
  email: string
  targetPosition: string
  techDirection: string
  city: string
  createTime: string
}

export interface AdminUserDetailVO extends AdminUserListItemVO {
  phone: string
  workYears: number
  jobStatus: string
  resumeCount: number
  interviewCount: number
  completedInterviewCount: number
  applicationCount: number
  trainingPlanCount: number
  trainingAnswerCount: number
  trainingReviewCount: number
}

export interface AdminUserQueryRequest {
  keyword?: string
  userStatus?: number
  userRole?: string
}

export interface AdminUserRoleUpdateRequest {
  userRole: string
}

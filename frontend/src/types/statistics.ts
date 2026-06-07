export interface HomeStatsVO {
  completedInterviews: number
  totalQuestions: number
  practiceDays: number
}

export interface AnalyticsOverviewVO {
  resumeCount: number
  jobCount: number
  interviewCount: number
  averageInterviewScore: number
  topMissingSkills: string[]
  nextActions: string[]
}

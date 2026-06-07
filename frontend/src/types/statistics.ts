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

export interface ReviewAnalyticsVO {
  radar: Record<string, number>
  topSkillGaps: Array<Record<string, string>>
  recentScoreTrend: Array<Record<string, string | number>>
  latestActionItems: string[]
}

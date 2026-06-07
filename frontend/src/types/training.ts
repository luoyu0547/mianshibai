export type TrainingPlanStatus = 'active' | 'completed' | 'archived'
export type TrainingQuestionStatus = 'pending' | 'answered' | 'reviewed' | 'mastered' | 'skipped'
export type TrainingDifficulty = 'easy' | 'medium' | 'hard'
export type MasteryLevel = 'weak' | 'basic' | 'good' | 'mastered'

export interface TrainingPlanGenerateRequest {
  sourceType?: string
  sourceId?: number
  targetDays?: number
  targetPosition?: string
}

export interface TrainingAnswerSubmitRequest {
  answerText: string
}

export interface TrainingAnswerReviewVO {
  id: number
  answerId: number
  totalScore: number
  accuracyScore: number
  clarityScore: number
  depthScore: number
  projectScore: number
  strengths: string[]
  mistakes: string[]
  missingPoints: string[]
  suggestions: string[]
  recommendedAnswer: string
  followUpQuestions: string[]
  masteryLevel: MasteryLevel
  createTime: string
}

export interface TrainingAnswerVO {
  id: number
  questionId: number
  answerText: string
  review: TrainingAnswerReviewVO | null
  createTime: string
}

export interface TrainingQuestionVO {
  id: number
  planId: number
  dayIndex: number
  title: string
  content: string
  topic: string
  skillTags: string[]
  difficulty: TrainingDifficulty
  sourceType: string
  referenceAnswer: string
  followUpQuestions: string[]
  status: TrainingQuestionStatus
  latestScore: number | null
  latestMasteryLevel: MasteryLevel | null
  createTime: string
  updateTime: string
}

export interface AlgorithmRecommendationVO {
  id: number
  planId: number
  category: string
  platform: string
  problemRef: string
  reason: string
  completed: number
}

export interface TrainingPlanVO {
  id: number
  title: string
  sourceType: string
  sourceId: number | null
  targetDays: number
  status: TrainingPlanStatus
  summary: string
  focusTopics: string[]
  questions: TrainingQuestionVO[]
  algorithmRecommendations: AlgorithmRecommendationVO[]
  createTime: string
  updateTime: string
}

export interface DashboardVO {
  todayPriorities: string[]
  applicationStats: {
    total: number
    pending: number
    interviewing: number
    offer: number
    failed: number
  } | null
  activePlan: TrainingPlanVO | null
  pendingQuestions: TrainingQuestionVO[]
  weakTopics: string[]
  algorithmRecommendations: AlgorithmRecommendationVO[]
}

export const TRAINING_DIFFICULTY_OPTIONS: { label: string; value: TrainingDifficulty }[] = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
]

export const MASTERY_LEVEL_OPTIONS: { label: string; value: MasteryLevel; color: string }[] = [
  { label: '薄弱', value: 'weak', color: '#e74c3c' },
  { label: '基础', value: 'basic', color: '#f39c12' },
  { label: '良好', value: 'good', color: '#2ecc71' },
  { label: '掌握', value: 'mastered', color: '#6C5CE7' },
]

export const QUESTION_STATUS_LABELS: Record<TrainingQuestionStatus, string> = {
  pending: '待作答',
  answered: '已作答',
  reviewed: '已批改',
  mastered: '已掌握',
  skipped: '已跳过',
}

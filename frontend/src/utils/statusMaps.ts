export type StatusVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'ai'

export interface StatusDescriptor {
  label: string
  variant: StatusVariant
  description?: string
}

export function getStatusDescriptor(
  map: Record<string, StatusDescriptor>,
  value: string | number | null | undefined,
  fallbackLabel = '未知',
): StatusDescriptor {
  if (value === null || value === undefined || value === '') {
    return { label: fallbackLabel, variant: 'muted' }
  }
  return map[String(value)] ?? { label: String(value), variant: 'muted' }
}

export const applicationStatusMap: Record<string, StatusDescriptor> = {
  pending_submit: { label: '待投递', variant: 'warning' },
  submitted: { label: '已投递', variant: 'info' },
  interviewing: { label: '面试中', variant: 'primary' },
  offer: { label: 'Offer', variant: 'success' },
  rejected: { label: '拒绝', variant: 'danger' },
  withdrawn: { label: '放弃', variant: 'muted' },
}

export const todoPriorityMap: Record<string, StatusDescriptor> = {
  low: { label: '低', variant: 'muted' },
  medium: { label: '中', variant: 'warning' },
  high: { label: '高', variant: 'danger' },
}

export const interviewStatusMap: Record<string, StatusDescriptor> = {
  created: { label: '待开始', variant: 'warning' },
  in_progress: { label: '进行中', variant: 'primary' },
  generating_report: { label: '生成报告中', variant: 'info' },
  completed: { label: '已完成', variant: 'success' },
  cancelled: { label: '已取消', variant: 'danger' },
}

export const trainingMasteryMap: Record<string, StatusDescriptor> = {
  weak: { label: '薄弱', variant: 'danger' },
  basic: { label: '基础', variant: 'warning' },
  good: { label: '良好', variant: 'info' },
  mastered: { label: '掌握', variant: 'success' },
}

export const trainingQuestionStatusMap: Record<string, StatusDescriptor> = {
  pending: { label: '待作答', variant: 'muted' },
  answered: { label: '已作答', variant: 'warning' },
  reviewed: { label: '已批改', variant: 'info' },
  mastered: { label: '已掌握', variant: 'success' },
  skipped: { label: '已跳过', variant: 'muted' },
}

export const trainingDifficultyMap: Record<string, StatusDescriptor> = {
  easy: { label: '简单', variant: 'success' },
  medium: { label: '中等', variant: 'warning' },
  hard: { label: '困难', variant: 'danger' },
}

export const userRoleMap: Record<string, StatusDescriptor> = {
  admin: { label: '管理员', variant: 'primary' },
  user: { label: '普通用户', variant: 'default' },
}

export const userStatusMap: Record<string, StatusDescriptor> = {
  '0': { label: '正常', variant: 'success' },
  '1': { label: '禁用', variant: 'danger' },
}

export const coachTaskStatusMap: Record<string, StatusDescriptor> = {
  pending: { label: '待完成', variant: 'warning' },
  completed: { label: '已完成', variant: 'success' },
  skipped: { label: '已跳过', variant: 'muted' },
}

export const aiProcessStatusMap: Record<string, StatusDescriptor> = {
  pending: { label: '排队中', variant: 'muted' },
  running: { label: '处理中', variant: 'primary' },
  completed: { label: '已完成', variant: 'success' },
  failed: { label: '失败', variant: 'danger' },
}

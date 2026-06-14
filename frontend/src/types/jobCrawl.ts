export type JobCrawlSourceType = 'company_career_page' | 'public_feed' | 'manual_url_list' | 'platform_entry_url'
export type JobCrawlScheduleType = 'manual' | 'daily' | 'weekly' | 'cron'
export type JobCrawlTaskStatus = 'enabled' | 'disabled'

export const JOB_CRAWL_SOURCE_TYPE_OPTIONS: Array<{ label: string; value: JobCrawlSourceType }> = [
  { label: '公司官网招聘页', value: 'company_career_page' },
  { label: '公开职位源', value: 'public_feed' },
  { label: '手动链接列表', value: 'manual_url_list' },
  { label: '平台入口URL', value: 'platform_entry_url' },
]

export const JOB_CRAWL_SCHEDULE_TYPE_OPTIONS: Array<{ label: string; value: JobCrawlScheduleType }> = [
  { label: '手动', value: 'manual' },
  { label: '每天', value: 'daily' },
  { label: '每周', value: 'weekly' },
  { label: '自定义CRON', value: 'cron' },
]

export interface AdminJobCrawlTaskVO {
  id: number
  name: string
  sourceType: JobCrawlSourceType
  sourceUrl: string
  keywords: string
  cities: string
  experienceLevels: string
  scheduleType: JobCrawlScheduleType
  cronExpression: string
  status: JobCrawlTaskStatus
  lastRunAt?: string | null
  nextRunAt?: string | null
  successCount?: number
  failedCount?: number
  latestStatus?: string
  remark?: string
}

export interface AdminJobCrawlRunVO {
  id: number
  taskId: number
  status: string
  startedAt?: string | null
  finishedAt?: string | null
  totalCount: number
  successCount: number
  duplicateCount: number
  failedCount: number
  errorMessage?: string
}

export interface AdminJobCrawlItemVO {
  id: number
  runId: number
  sourceUrl: string
  jobId?: number | null
  status: string
  errorMessage: string
  rawTitle: string
  rawCompanyName: string
}

export interface AdminJobCrawlTaskCreateRequest {
  name: string
  sourceType: JobCrawlSourceType
  sourceUrl?: string
  keywords?: string
  cities?: string
  experienceLevels?: string
  scheduleType?: JobCrawlScheduleType
  cronExpression?: string
  remark?: string
}

export interface AdminJobCrawlTaskUpdateRequest extends Partial<AdminJobCrawlTaskCreateRequest> {}

export interface AdminJobCrawlTaskQueryRequest {
  name?: string
  sourceType?: string
  status?: string
}

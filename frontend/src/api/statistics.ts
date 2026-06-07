import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { HomeStatsVO, AnalyticsOverviewVO, ReviewAnalyticsVO } from '@/types/statistics'

export function getHomeStats() {
  return request.get<BaseResponse<HomeStatsVO>>('/api/statistics/home')
}

export function getAnalyticsOverview() {
  return request.get<BaseResponse<AnalyticsOverviewVO>>('/api/statistics/analytics/overview')
}

export function getReviewAnalytics() {
  return request.get<BaseResponse<ReviewAnalyticsVO>>('/api/statistics/analytics/review')
}

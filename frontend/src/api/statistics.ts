import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { HomeStatsVO } from '@/types/statistics'

export function getHomeStats() {
  return request.get<BaseResponse<HomeStatsVO>>('/api/statistics/home')
}

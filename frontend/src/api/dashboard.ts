import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { DashboardVO } from '@/types/training'

export function getDashboard() {
  return request.get<BaseResponse<DashboardVO>>('/api/dashboard')
}

import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  AdminOverviewVO,
  AdminUserDetailVO,
  AdminUserListItemVO,
  AdminUserQueryRequest,
  AdminUserRoleUpdateRequest,
} from '@/types/admin'

export function getAdminOverview() {
  return request.get<BaseResponse<AdminOverviewVO>>('/api/admin/overview')
}

export function listAdminUsers(params?: AdminUserQueryRequest) {
  return request.get<BaseResponse<AdminUserListItemVO[]>>('/api/admin/users', { params })
}

export function getAdminUser(id: number) {
  return request.get<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}`)
}

export function disableAdminUser(id: number) {
  return request.put<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}/disable`)
}

export function enableAdminUser(id: number) {
  return request.put<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}/enable`)
}

export function updateAdminUserRole(id: number, data: AdminUserRoleUpdateRequest) {
  return request.put<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}/role`, data)
}

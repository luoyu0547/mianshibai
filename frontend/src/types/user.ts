// src/types/user.ts

export interface LoginUserVO {
  id: number
  userAccount: string
  userName: string
  userAvatar: string
  userRole: string
  userStatus: number
  email: string
  phone: string
  targetPosition: string
  techDirection: string
  workYears: number
  city: string
  jobStatus: string
  createTime: string
}

export interface UserLoginVO {
  token: string
  user: LoginUserVO
}

export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export interface LoginRequest {
  userAccount: string
  userPassword: string
}

export interface RegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

export interface UpdateProfileRequest {
  userName?: string
  userAvatar?: string
  targetPosition?: string
  techDirection?: string
  workYears?: number
  city?: string
  jobStatus?: string
}

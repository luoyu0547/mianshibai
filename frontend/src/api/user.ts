// src/api/user.ts
import request from '@/utils/request'
import type {
  BaseResponse,
  LoginRequest,
  RegisterRequest,
  UserLoginVO,
  LoginUserVO,
  UpdateProfileRequest,
  FileUploadVO,
} from '@/types/user'

export function login(data: LoginRequest) {
  return request.post<BaseResponse<UserLoginVO>>('/api/user/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<BaseResponse<number>>('/api/user/register', data)
}

export function getCurrentUser() {
  return request.get<BaseResponse<LoginUserVO>>('/api/user/current')
}

export function updateProfile(data: UpdateProfileRequest) {
  return request.put<BaseResponse<LoginUserVO>>('/api/user/profile', data)
}

export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<BaseResponse<FileUploadVO>>('/api/file/avatar', formData)
}

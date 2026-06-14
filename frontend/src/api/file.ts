// src/api/file.ts
import request from '@/utils/request'
import type { BaseResponse, FileUploadVO } from '@/types/user'

export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<BaseResponse<FileUploadVO>>('/api/file/avatar', formData)
}

// src/utils/request.ts
import axios, { type AxiosError, type AxiosResponse, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 180000,
})

// 请求拦截器：附加 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('mianshiba_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：解包 + 错误处理
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      // 40100 未登录
      if (data.code === 40100) {
        localStorage.removeItem('mianshiba_token')
        router.push('/login')
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error: AxiosError) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default request as unknown as {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
}

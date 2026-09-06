import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { createDiscreteApi } from 'naive-ui'

/** 登录态持久化键（auth store 与请求拦截器共用，唯一出处在此） */
export const TOKEN_KEY = 'community.token'

/** 后端统一响应体（见 backend com.community.common.Result） */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

const { message } = createDiscreteApi(['message'])

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function redirectToLogin() {
  if (!window.location.pathname.startsWith('/login')) {
    window.location.assign('/login')
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult
    if (body.code === 0) {
      return response
    }
    // 业务失败：HTTP 200 但 code 非 0（理论上后端会用错误 HTTP 状态，这里兜底）
    message.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data as ApiResult | undefined
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      redirectToLogin()
    }
    message.error(body?.message || '网络异常，请稍后再试')
    return Promise.reject(error)
  }
)

/** 便捷取数：返回统一响应体中的 data */
export async function unwrap<T>(promise: Promise<AxiosResponse<ApiResult<T>>>): Promise<T> {
  const resp = await promise
  return resp.data.data
}

export default request

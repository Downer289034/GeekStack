import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { createDiscreteApi } from 'naive-ui'
import { clearTokens, getAccessToken, getRefreshToken, setTokens, type TokenPair } from '@/api/tokenStorage'

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
  const token = getAccessToken()
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

/** 单飞刷新：并发 401 只触发一次 refresh，其余等待复用结果，避免刷新风暴。 */
let refreshingPromise: Promise<string | null> | null = null

function tryRefreshOnce(): Promise<string | null> {
  if (!refreshingPromise) {
    refreshingPromise = (async () => {
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        return null
      }
      try {
        // 独立实例：不经过本模块拦截器，避免递归刷新
        const resp = await axios.post<ApiResult<TokenPair>>(
          '/api/v1/auth/refresh',
          { refreshToken },
          { timeout: 15000 }
        )
        const pair = resp.data.data
        setTokens(pair)
        return pair.accessToken
      } catch {
        return null
      }
    })()
    void refreshingPromise.finally(() => {
      refreshingPromise = null
    })
  }
  return refreshingPromise
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

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
  async (error: AxiosError<ApiResult>) => {
    const config = error.config as RetriableConfig | undefined
    const status = error.response?.status
    const isAuthApi = config?.url?.includes('/v1/auth/')

    if (status === 401 && config && !config._retried && !isAuthApi) {
      config._retried = true
      const newToken = await tryRefreshOnce()
      if (newToken) {
        config.headers.Authorization = `Bearer ${newToken}`
        return request.request(config)
      }
      clearTokens()
      redirectToLogin()
    } else if (status === 401) {
      clearTokens()
      redirectToLogin()
    }

    message.error(error.response?.data?.message || '网络异常，请稍后再试')
    return Promise.reject(error)
  }
)

/** 便捷取数：返回统一响应体中的 data */
export async function unwrap<T>(promise: Promise<AxiosResponse<ApiResult<T>>>): Promise<T> {
  const resp = await promise
  return resp.data.data
}

export default request

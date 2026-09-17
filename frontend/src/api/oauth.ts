import request, { unwrap } from '@/api/request'
import axios from 'axios'
import { getAccessToken, type TokenPair } from '@/api/tokenStorage'

export type OAuthProvider = 'GITHUB' | 'GITEE'

const STATE_MIN_HEX = 32

/** sessionStorage：FE 生成的 CSRF state（比对闭环以本 key 为准） */
export function oauthStateStorageKey(provider: OAuthProvider): string {
  return `oauth_csrf_state_${provider.toLowerCase()}`
}

/** localStorage：BE callback 写入的 pending（含 code+state） */
export function oauthPendingStorageKey(provider: OAuthProvider): string {
  return `oauth_pending_${provider.toLowerCase()}`
}

/**
 * 生成 ≥32 hex（128 bit）state，符合 BE 白名单：仅 [0-9a-fA-F]，长度 32–128。
 */
export function generateOAuthState(): string {
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}

/**
 * 发起登录/绑定：生成 state → sessionStorage → 带 ?state= 跳转 BE login。
 * BE 仅白名单接纳并存 Redis；比对闭环由回调页读 sessionStorage 完成。
 */
export function beginOAuth(provider: OAuthProvider): void {
  const state = generateOAuthState()
  sessionStorage.setItem(oauthStateStorageKey(provider), state)
  const url = `/api/v1/oauth/login/${provider}?state=${encodeURIComponent(state)}`
  window.location.assign(url)
}

/** 读取并清除期望 state；缺失返回 null */
export function takeExpectedOAuthState(provider: OAuthProvider): string | null {
  const key = oauthStateStorageKey(provider)
  const value = sessionStorage.getItem(key)
  sessionStorage.removeItem(key)
  if (!value || value.length < STATE_MIN_HEX) {
    return null
  }
  return value
}

/**
 * 转交授权码 + state。
 * 已登录时附带 Bearer（绑定语义）；匿名则登录/注册语义。
 * 使用原生 axios（避免 refresh 拦截器干扰 code 交换）。
 */
export function submitOAuthCode(
  provider: OAuthProvider,
  code: string,
  state: string
): Promise<TokenPair> {
  const headers: Record<string, string> = {}
  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return axios
    .post<{ code: number; message?: string; data: TokenPair }>(
      `/api/v1/oauth/${provider}/code`,
      { code, state },
      { headers, timeout: 15000 }
    )
    .then((resp) => {
      if (resp.data.code !== 0) {
        return Promise.reject(new Error(resp.data.message || 'OAuth 失败'))
      }
      return resp.data.data
    })
}

/** 解绑第三方账号（需登录；最后绑定保护由后端拒绝） */
export function unbindOAuth(provider: OAuthProvider): Promise<void> {
  return unwrap<void>(request.post(`/v1/oauth/unbind/${provider}`))
}

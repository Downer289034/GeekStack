import request, { unwrap } from '@/api/request'
import axios from 'axios'
import type { TokenPair } from '@/api/tokenStorage'

export type OAuthProvider = 'GITHUB' | 'GITEE'

/** 转交授权码：使用原生 axios（不经过 JWT 拦截器，避免带错 token 导致 401）。 */
export function submitOAuthCode(provider: OAuthProvider, code: string): Promise<TokenPair> {
  return axios
    .post<{ code: number; data: TokenPair }>(`/api/v1/oauth/${provider}/code`, { code })
    .then((resp) => resp.data.data)
}

/** 解绑第三方账号（需登录；最后绑定保护由后端拒绝） */
export function unbindOAuth(provider: OAuthProvider): Promise<void> {
  return unwrap<void>(request.post(`/v1/oauth/unbind/${provider}`))
}

/** 跳转平台授权（绑定/登录共用后端入口） */
export function oauthLoginUrl(provider: OAuthProvider): string {
  return `/api/v1/oauth/login/${provider}`
}

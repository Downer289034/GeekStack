import request, { unwrap } from '@/api/request'
import type { TokenPair } from '@/api/tokenStorage'

export type OAuthProvider = 'GITHUB' | 'GITEE'

/** 获取平台授权页地址（后端拼好 client_id/scope/redirect_uri） */
export function fetchAuthorizeUrl(provider: OAuthProvider): Promise<string> {
  return unwrap<{ authorizeUrl: string }>(request.get(`/v1/oauth/authorize-url/${provider}`))
    .then((data) => data.authorizeUrl)
}

/**
 * 转交授权码（双语义）：请求拦截器带 JWT=绑定语义；不带=登录/注册语义。
 * 成功返回令牌对（两种语义都返回，绑定后保持原登录态刷新）。
 */
export function submitOAuthCode(provider: OAuthProvider, code: string): Promise<TokenPair> {
  return unwrap<TokenPair>(request.post(`/v1/oauth/${provider}/code`, { code }))
}

import axios from 'axios'
import type { TokenPair } from '@/api/tokenStorage'

export type OAuthProvider = 'GITHUB' | 'GITEE'

/** 转交授权码：使用原生 axios（不经过 JWT 拦截器，避免带错 token 导致 401）。 */
export function submitOAuthCode(provider: OAuthProvider, code: string): Promise<TokenPair> {
  return axios
    .post<{ code: number; data: TokenPair }>(`/api/v1/oauth/${provider}/code`, { code })
    .then((resp) => resp.data.data)
}

/** 令牌持久化：localStorage 读写唯一出处，auth store 与 axios 拦截器共用。 */
export const TOKEN_KEY = 'community.token'
export const REFRESH_TOKEN_KEY = 'community.refreshToken'

export interface TokenPair {
  accessToken: string
  refreshToken: string
}

export function getAccessToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? ''
}

export function setTokens(pair: TokenPair): void {
  localStorage.setItem(TOKEN_KEY, pair.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

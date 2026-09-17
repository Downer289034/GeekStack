import request, { unwrap } from '@/api/request'
import type { TokenPair } from '@/api/tokenStorage'

/** 用户公开信息（与后端 UserResponse 对应；T-PROFILE 扩展字段可选） */
export interface UserProfile {
  id: number
  username: string
  email: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  role: string
  createdAt: string
  /** 是否已设置本地密码（第三方注册可能为 false） */
  hasPassword?: boolean
  /** OAuth 绑定状态 */
  oauthBindings?: {
    github: boolean
    gitee: boolean
  }
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
}

export interface UpdateMePayload {
  nickname?: string
  bio?: string | null
  avatarUrl?: string | null
}

export interface UpdatePasswordPayload {
  /** 已有密码时必填；无密用户首次设置可省略 */
  oldPassword?: string | null
  newPassword: string
}

export function register(payload: RegisterPayload): Promise<TokenPair> {
  return unwrap<TokenPair>(request.post('/v1/auth/register', payload))
}

export function login(username: string, password: string): Promise<TokenPair> {
  return unwrap<TokenPair>(request.post('/v1/auth/login', { username, password }))
}

export function fetchMe(): Promise<UserProfile> {
  return unwrap<UserProfile>(request.get('/v1/users/me'))
}

export function updateMe(payload: UpdateMePayload): Promise<UserProfile> {
  return unwrap<UserProfile>(request.put('/v1/users/me', payload))
}

export function updatePassword(payload: UpdatePasswordPayload): Promise<void> {
  return unwrap<void>(request.put('/v1/users/me/password', payload))
}

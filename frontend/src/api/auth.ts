import request, { unwrap } from '@/api/request'
import type { TokenPair } from '@/api/tokenStorage'

/** 用户公开信息（与后端 UserResponse 对应） */
export interface UserProfile {
  id: number
  username: string
  email: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  role: string
  createdAt: string
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
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

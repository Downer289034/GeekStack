import request, { unwrap } from '@/api/request'
import type { PostCard } from '@/api/forum'
import type { ShareResponse } from '@/api/share'

/** 主页资料卡（不含邮箱） */
export interface Profile {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  role: string
  createdAt: string
}

/** 主页聚合（与后端 ProfileAggregateResponse 对应） */
export interface ProfileAggregate {
  profile: Profile
  shares: ShareResponse[]
  posts: PostCard[]
}

export function fetchProfile(username: string): Promise<ProfileAggregate> {
  return unwrap<ProfileAggregate>(request.get(`/v1/users/${encodeURIComponent(username)}/profile`))
}

import request, { unwrap } from '@/api/request'
import type { PostCard } from '@/api/forum'
import type { PageResult, ShareCard } from '@/api/share'
import type { SkillCard } from '@/api/skill'

export type SearchType = 'post' | 'share' | 'skill'

/** G1：关键字搜索（type 仅 post|share|skill，不做 all） */
export function searchContent(
  q: string,
  type: SearchType,
  current = 1,
  size = 10
): Promise<PageResult<PostCard | ShareCard | SkillCard>> {
  return unwrap<PageResult<PostCard | ShareCard | SkillCard>>(
    request.get('/v1/search', {
      params: { q, type, current, size }
    })
  )
}

/** G2：标签云（可见帖 + PUBLISHED share/skill，与 T-BE-4.2 对齐） */
export interface TagCount {
  name: string
  count: number
}

export function fetchTagCloud(): Promise<TagCount[]> {
  return unwrap<TagCount[]>(request.get('/v1/tags'))
}

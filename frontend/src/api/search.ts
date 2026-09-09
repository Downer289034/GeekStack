import request, { unwrap } from '@/api/request'
import type { PostCard } from '@/api/forum'
import type { PageResult, ShareCard } from '@/api/share'

export type SearchType = 'post' | 'share'

/** G1：关键字搜索（type 仅 post|share，不做 all） */
export function searchContent(
  q: string,
  type: SearchType,
  current = 1,
  size = 10
): Promise<PageResult<PostCard | ShareCard>> {
  return unwrap<PageResult<PostCard | ShareCard>>(
    request.get('/v1/search', {
      params: { q, type, current, size }
    })
  )
}

/** G2：项目标签云（share-only） */
export interface TagCount {
  name: string
  count: number
}

export function fetchTagCloud(): Promise<TagCount[]> {
  return unwrap<TagCount[]>(request.get('/v1/tags'))
}

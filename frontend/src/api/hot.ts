import request, { unwrap } from '@/api/request'
import type { PostCard } from '@/api/forum'

/** 热帖榜（T21 / T-FE-2）：公开，服务端 60s 缓存 */
export function fetchHotPosts(size = 10): Promise<PostCard[]> {
  return unwrap<PostCard[]>(request.get('/v1/hot', { params: { size } }))
}

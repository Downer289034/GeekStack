import request, { unwrap } from '@/api/request'
import type { PageResult } from '@/api/share'

/** 帖子列表卡片（与后端 PostCardResponse 对应） */
export interface PostCard {
  id: number
  userId: number
  categoryId: number
  title: string
  tags: string[]
  viewCount: number
  replyCount: number
  lastReplyAt: string | null
  isPinned: number
  isHot: number
  createdAt: string
}

/** 帖子详情（与后端 PostDetailResponse 对应） */
export interface PostDetail {
  id: number
  userId: number
  categoryId: number
  title: string
  contentMd: string
  tags: string[]
  viewCount: number
  replyCount: number
  lastReplyAt: string | null
  isPinned: number
  isHot: number
  createdAt: string
}

export interface Category {
  id: number
  name: string
  slug: string
  sortOrder: number
}

/** 回帖节点（children 仅一级） */
export interface CommentNode {
  id: number
  userId: number
  parentId: number | null
  contentMd: string
  likeCount: number
  createdAt: string
  children: CommentNode[]
}

export function fetchCategories(): Promise<Category[]> {
  return unwrap<Category[]>(request.get('/v1/posts/categories'))
}

export function pagePosts(
  current: number,
  size: number,
  category?: string,
  sort?: string,
  tag?: string
): Promise<PageResult<PostCard>> {
  return unwrap<PageResult<PostCard>>(
    request.get('/v1/posts', {
      params: {
        current,
        size,
        category: category || undefined,
        sort: sort || undefined,
        tag: tag || undefined
      }
    })
  )
}

export function fetchPost(id: number | string): Promise<PostDetail> {
  return unwrap<PostDetail>(request.get(`/v1/posts/${id}`))
}

export function createPost(payload: {
  categoryId: number
  title: string
  contentMd: string
  tags?: string[]
}): Promise<PostDetail> {
  return unwrap<PostDetail>(request.post('/v1/posts', payload))
}

export function updatePost(
  id: number,
  payload: { title: string; contentMd: string; tags?: string[] }
): Promise<PostDetail> {
  return unwrap<PostDetail>(request.put(`/v1/posts/${id}`, payload))
}

export function deletePost(id: number): Promise<void> {
  return unwrap<void>(request.delete(`/v1/posts/${id}`))
}

export function fetchComments(targetType: 'posts' | 'shares', targetId: number | string): Promise<CommentNode[]> {
  return unwrap<CommentNode[]>(request.get(`/v1/${targetType}/${targetId}/comments`))
}

export function createComment(
  targetType: 'posts' | 'shares',
  targetId: number | string,
  contentMd: string,
  parentId?: number
): Promise<CommentNode> {
  return unwrap<CommentNode>(
    request.post(`/v1/${targetType}/${targetId}/comments`, {
      contentMd,
      parentId: parentId ?? undefined
    })
  )
}

export function deleteComment(targetType: 'posts' | 'shares', commentId: number): Promise<void> {
  return unwrap<void>(request.delete(`/v1/${targetType}/${commentId}/comments`))
}

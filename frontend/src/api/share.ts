import request, { unwrap } from '@/api/request'

/** 项目分享载荷（与后端 SharePayload 对应） */
export interface SharePayload {
  title: string
  summary: string
  contentMd: string
  repoUrl?: string
  demoUrl?: string
  tags?: string[]
}

/** 项目分享响应（与后端 ShareResponse 对应） */
export interface ShareResponse {
  id: number
  userId: number
  title: string
  summary: string
  contentMd: string
  repoUrl: string | null
  demoUrl: string | null
  tags: string[] | null
  status: 'PUBLISHED' | 'HIDDEN'
  viewCount: number
  createdAt: string
}

export function createShare(payload: SharePayload): Promise<ShareResponse> {
  return unwrap<ShareResponse>(request.post('/v1/shares', payload))
}

export function updateShare(id: number, payload: SharePayload): Promise<ShareResponse> {
  return unwrap<ShareResponse>(request.put(`/v1/shares/${id}`, payload))
}

export function hideShare(id: number): Promise<ShareResponse> {
  return unwrap<ShareResponse>(request.post(`/v1/shares/${id}/hide`))
}

import request, { unwrap } from '@/api/request'
import type { PageResult } from '@/api/share'

/** 发布/编辑 Skill（与计划冻结契约对齐） */
export interface SkillPayload {
  title: string
  summary?: string
  promptMd: string
  modelNotes?: string
  tags?: string[]
}

/** Skill 详情 */
export interface SkillResponse {
  id: number
  userId: number
  title: string
  summary: string | null
  promptMd: string
  modelNotes: string | null
  tags: string[] | null
  status: 'PUBLISHED' | 'HIDDEN'
  viewCount: number
  likeCount?: number
  createdAt: string
}

/** 广场卡片 */
export interface SkillCard {
  id: number
  userId?: number
  title: string
  summary: string | null
  modelNotes: string | null
  tags: string[] | null
  viewCount: number
  likeCount?: number
  createdAt: string
}

export function pageSkills(current: number, size: number, tag?: string): Promise<PageResult<SkillCard>> {
  return unwrap<PageResult<SkillCard>>(
    request.get('/v1/skills', { params: { current, size, tag: tag || undefined } })
  )
}

export function fetchSkill(id: number | string): Promise<SkillResponse> {
  return unwrap<SkillResponse>(request.get(`/v1/skills/${id}`))
}

export function createSkill(payload: SkillPayload): Promise<SkillResponse> {
  return unwrap<SkillResponse>(request.post('/v1/skills', payload))
}

export function updateSkill(id: number, payload: SkillPayload): Promise<SkillResponse> {
  return unwrap<SkillResponse>(request.put(`/v1/skills/${id}`, payload))
}

export function hideSkill(id: number): Promise<void> {
  return unwrap<void>(request.delete(`/v1/skills/${id}`))
}

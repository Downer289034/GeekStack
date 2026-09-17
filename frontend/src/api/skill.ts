import request, { unwrap } from '@/api/request'
import type { PageResult } from '@/api/share'

/** 发布/编辑 Skill（与后端 SkillPayload 对齐） */
export interface SkillPayload {
  title: string
  summary: string
  contentMd: string
  modelAdaptationMd?: string
  tags?: string[]
}

/** Skill 详情（与后端 SkillResponse 对齐） */
export interface SkillResponse {
  id: number
  userId: number
  title: string
  summary: string
  contentMd: string
  modelAdaptationMd: string | null
  tags: string[] | null
  status: 'PUBLISHED' | 'HIDDEN' | string
  viewCount: number
  likeCount: number
  createdAt: string
}

/** 广场卡片（与后端 SkillCardResponse 对齐） */
export interface SkillCard {
  id: number
  userId: number
  title: string
  summary: string
  modelAdaptationMd: string | null
  tags: string[] | null
  viewCount: number
  likeCount: number
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

export function hideSkill(id: number): Promise<SkillResponse> {
  return unwrap<SkillResponse>(request.post(`/v1/skills/${id}/hide`))
}

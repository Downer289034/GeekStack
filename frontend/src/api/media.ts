import request, { unwrap, type ApiResult } from '@/api/request'

/** 与后端 MediaController scene 对齐 */
export type MediaScene = 'editor' | 'cover' | 'avatar'

/** 与后端 MediaUploadResponse 对齐 */
export interface MediaUploadResult {
  url: string
  key: string
  contentType: string
  size: number
}

/**
 * 上传单图：JWT + multipart `file`，可选 scene。
 * 联调入口见 docs/M7-2-dev-smoke.md。
 */
export function uploadMedia(file: File, scene: MediaScene = 'editor'): Promise<MediaUploadResult> {
  const form = new FormData()
  form.append('file', file)
  form.append('scene', scene)
  return unwrap(
    request.post<ApiResult<MediaUploadResult>>('/v1/media/upload', form, {
      timeout: 60000
    })
  )
}

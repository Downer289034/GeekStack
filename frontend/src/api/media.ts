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
 * 将上传/资料中的媒体 URL 转为浏览器可直接加载的地址。
 * - 已是相对 `/media/...`：原样（Vite 代理 / 生产同域）
 * - 绝对 URL 且 path 为 `/media/...`：改为同源相对路径，避免本机打不开 9080
 * - blob/data：原样
 */
export function resolveMediaUrl(url: string | null | undefined): string {
  if (!url) return ''
  const trimmed = url.trim()
  if (!trimmed) return ''
  if (/^(blob:|data:)/i.test(trimmed)) return trimmed
  if (trimmed.startsWith('/')) return trimmed
  try {
    const parsed = new URL(trimmed)
    if (parsed.pathname.startsWith('/media/')) {
      return `${parsed.pathname}${parsed.search}`
    }
  } catch {
    // not absolute
  }
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  return `/${trimmed.replace(/^\.?\/+/, '')}`
}

/**
 * 探测图片 URL 是否可被浏览器加载（失败勿当上传成功）。
 */
export function probeImageUrl(url: string, timeoutMs = 8000): Promise<boolean> {
  const src = resolveMediaUrl(url)
  if (!src) return Promise.resolve(false)
  return new Promise((resolve) => {
    const img = new Image()
    const timer = window.setTimeout(() => {
      img.onload = null
      img.onerror = null
      resolve(false)
    }, timeoutMs)
    img.onload = () => {
      window.clearTimeout(timer)
      resolve(true)
    }
    img.onerror = () => {
      window.clearTimeout(timer)
      resolve(false)
    }
    img.src = src
  })
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

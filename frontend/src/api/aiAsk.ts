/**
 * T-AI-4：POST /api/v1/ai/ask 的 SSE 客户端。
 * EventSource 不支持 POST，用 fetch + ReadableStream。
 * 令牌与刷新走 tokenStorage（与 axios 拦截器同一出处）；流式响应不能走 axios。
 */
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/api/tokenStorage'

export interface AiSourceRef {
  title: string
  url: string
  contentId?: string
  score?: number
}

export interface AiUsage {
  prompt_tokens?: number
  completion_tokens?: number
  total_tokens?: number
}

export type AiStreamEvent =
  | { type: 'token'; text: string }
  | { type: 'sources'; sources: AiSourceRef[] }
  | { type: 'done'; usage?: AiUsage }

export class AiAskError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'AiAskError'
    this.status = status
  }
}

export const AI_UNAVAILABLE = 'AI 助手暂时不可用，请稍后再试'
export const AI_RATE_LIMIT = '提问过于频繁，请稍后再试'

interface SseBuffer {
  rest: string
}

/** 消费一段 SSE 文本，返回未完成的尾部。 */
export function consumeSseChunk(state: SseBuffer, chunk: string, onEvent: (ev: AiStreamEvent) => void): void {
  state.rest += chunk
  const blocks = state.rest.split(/\n\n/)
  state.rest = blocks.pop() ?? ''
  for (const block of blocks) {
    const ev = parseSseBlock(block)
    if (ev) onEvent(ev)
  }
}

function parseSseBlock(block: string): AiStreamEvent | null {
  let event = 'message'
  const dataLines: string[] = []
  for (const raw of block.split('\n')) {
    const line = raw.replace(/\r$/, '')
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).replace(/^ /, ''))
    }
  }
  if (dataLines.length === 0) return null
  let data: Record<string, unknown>
  try {
    data = JSON.parse(dataLines.join('\n')) as Record<string, unknown>
  } catch {
    return null
  }
  if (event === 'token') {
    const text = typeof data.delta === 'string' ? data.delta : typeof data.text === 'string' ? data.text : ''
    return { type: 'token', text }
  }
  if (event === 'sources') {
    const raw = Array.isArray(data.sources) ? data.sources : []
    const sources: AiSourceRef[] = []
    for (const item of raw) {
      if (!item || typeof item !== 'object') continue
      const row = item as Record<string, unknown>
      if (typeof row.title !== 'string' || typeof row.url !== 'string') continue
      sources.push({
        title: row.title,
        url: row.url,
        contentId: typeof row.content_id === 'string' ? row.content_id : undefined,
        score: typeof row.score === 'number' ? row.score : undefined
      })
    }
    return { type: 'sources', sources }
  }
  if (event === 'done') {
    const usage = data.usage
    return {
      type: 'done',
      usage: usage && typeof usage === 'object' ? (usage as AiUsage) : undefined
    }
  }
  return null
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return null
  try {
    const resp = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })
    if (!resp.ok) return null
    const body = (await resp.json()) as {
      code?: number
      data?: { accessToken: string; refreshToken: string }
    }
    if (body.code !== 0 || !body.data?.accessToken) return null
    setTokens(body.data)
    return body.data.accessToken
  } catch {
    return null
  }
}

async function errorFromResponse(resp: Response): Promise<AiAskError> {
  let message = ''
  try {
    const body = (await resp.json()) as { message?: string }
    message = typeof body.message === 'string' ? body.message : ''
  } catch {
    message = ''
  }
  if (resp.status === 503) return new AiAskError(503, message || AI_UNAVAILABLE)
  if (resp.status === 429) return new AiAskError(429, message || AI_RATE_LIMIT)
  return new AiAskError(resp.status, message || '请求失败')
}

async function postAsk(question: string, token: string, signal?: AbortSignal): Promise<Response> {
  return fetch('/api/v1/ai/ask', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // 兼收 JSON：上游宕机/限流走 BusinessException JSON；仅 SSE 时 Spring 无法协商 503/429 体
      Accept: 'text/event-stream, application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ question }),
    signal
  })
}

/**
 * 提问并解析 SSE。401 时刷新令牌重试一次。
 */
export async function askStream(
  question: string,
  onEvent: (ev: AiStreamEvent) => void,
  signal?: AbortSignal
): Promise<void> {
  const token = getAccessToken()
  if (!token) throw new AiAskError(401, '请先登录')

  let resp = await postAsk(question, token, signal)
  if (resp.status === 401) {
    const next = await refreshAccessToken()
    if (!next) {
      clearTokens()
      throw new AiAskError(401, '请先登录')
    }
    resp = await postAsk(question, next, signal)
  }
  if (!resp.ok) throw await errorFromResponse(resp)
  if (!resp.body) throw new AiAskError(503, AI_UNAVAILABLE)

  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  const state: SseBuffer = { rest: '' }
  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      consumeSseChunk(state, decoder.decode(value, { stream: true }), onEvent)
    }
    consumeSseChunk(state, decoder.decode(), onEvent)
    if (state.rest.trim()) {
      consumeSseChunk({ rest: '' }, `${state.rest}\n\n`, onEvent)
    }
  } finally {
    reader.releaseLock()
  }
}

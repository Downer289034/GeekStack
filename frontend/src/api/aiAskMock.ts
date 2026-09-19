/**
 * T-AI-4-PRE：本地 mock「SSE」流（定时器吐 token）。
 * T-AI-3 落地后替换为真实 POST /api/v1/ai/ask SSE，本文件可删或改薄适配层。
 */

export interface AiSourceRef {
  title: string
  url: string
}

export type AiStreamEvent =
  | { type: 'token'; text: string }
  | { type: 'sources'; sources: AiSourceRef[] }
  | { type: 'done' }

const CHUNK_MS = 28

function buildMockAnswer(question: string): { body: string; sources: AiSourceRef[] } {
  const q = question.trim() || '（空问题）'
  const body = [
    `这是 **mock 流式回答**（T-AI-4-PRE）。你问的是：`,
    ``,
    `> ${q}`,
    ``,
    `真实 RAG 接入后，这里会改为基于社区语料的引用回答。当前仅验证：`,
    ``,
    `1. token 流渲染不卡顿`,
    `2. Markdown 走全站唯一出口`,
    `3. 来源卡片占位（可点）`,
    ``,
    `—— 极客栈 AI 助手骨架`
  ].join('\n')

  const sources: AiSourceRef[] = [
    { title: 'Skill 广场', url: '/skills' },
    { title: '技术论坛', url: '/forum' },
    { title: '使用说明（占位）', url: '/' }
  ]
  return { body, sources }
}

/** 将文本切成小块，模拟 token 到达 */
function chunkText(text: string): string[] {
  const parts: string[] = []
  let i = 0
  while (i < text.length) {
    const n = 1 + Math.floor(Math.random() * 3)
    parts.push(text.slice(i, i + n))
    i += n
  }
  return parts
}

/**
 * 模拟 SSE：按块回调；可用 AbortSignal 中止。
 */
export function mockAskStream(
  question: string,
  onEvent: (ev: AiStreamEvent) => void,
  signal?: AbortSignal
): Promise<void> {
  const { body, sources } = buildMockAnswer(question)
  const chunks = chunkText(body)

  return new Promise((resolve, reject) => {
    let idx = 0
    let timer = 0

    const cleanup = () => {
      if (timer) window.clearTimeout(timer)
      timer = 0
    }

    const onAbort = () => {
      cleanup()
      signal?.removeEventListener('abort', onAbort)
      reject(new DOMException('Aborted', 'AbortError'))
    }

    if (signal?.aborted) {
      onAbort()
      return
    }
    signal?.addEventListener('abort', onAbort)

    const tick = () => {
      if (signal?.aborted) return
      if (idx < chunks.length) {
        onEvent({ type: 'token', text: chunks[idx++] })
        timer = window.setTimeout(tick, CHUNK_MS)
        return
      }
      onEvent({ type: 'sources', sources })
      onEvent({ type: 'done' })
      cleanup()
      signal?.removeEventListener('abort', onAbort)
      resolve()
    }

    timer = window.setTimeout(tick, 120)
  })
}

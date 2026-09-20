import type { AiSourceRef } from '@/api/aiAsk'

export interface AiChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: AiSourceRef[]
  createdAt: number
}

const STORAGE_KEY = 'geekstack.ai.chat.v1'
const MAX_MESSAGES = 80

export function loadAiChatHistory(): AiChatMessage[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((m): m is AiChatMessage => {
        if (!m || typeof m !== 'object') return false
        const row = m as AiChatMessage
        return (
          typeof row.id === 'string' &&
          (row.role === 'user' || row.role === 'assistant') &&
          typeof row.content === 'string' &&
          typeof row.createdAt === 'number'
        )
      })
      .slice(-MAX_MESSAGES)
  } catch {
    return []
  }
}

export function saveAiChatHistory(messages: AiChatMessage[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(messages.slice(-MAX_MESSAGES)))
  } catch {
    // quota / private mode：静默
  }
}

export function clearAiChatHistory(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
}

export function newMessageId(): string {
  return `m_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
}

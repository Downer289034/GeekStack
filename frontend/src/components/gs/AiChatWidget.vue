<script setup lang="ts">
/**
 * T-AI-4：AI 浮窗。SSE 走 POST /api/v1/ai/ask（fetch + ReadableStream）。
 */
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NEmpty, NIcon } from 'naive-ui'
import {
  ChatbubbleEllipsesOutline,
  CloseOutline,
  SendOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { AiAskError, AI_RATE_LIMIT, AI_UNAVAILABLE, askStream, type AiSourceRef } from '@/api/aiAsk'
import GsButton from '@/components/gs/GsButton.vue'
import { useAuthStore } from '@/stores/auth'
import {
  clearAiChatHistory,
  loadAiChatHistory,
  newMessageId,
  saveAiChatHistory,
  type AiChatMessage
} from '@/utils/aiChatHistory'
import { renderMarkdown } from '@/utils/markdown'

const HIDDEN_PREFIXES = ['/login', '/oauth/callback']
const MOBILE_MQ = '(max-width: 768px)'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const open = ref(false)
const input = ref('')
const sending = ref(false)
const loginHint = ref(false)
const panelError = ref<'down' | 'limit' | null>(null)
const messages = ref<AiChatMessage[]>([])
const listRef = ref<HTMLElement | null>(null)
const isMobile = ref(false)
const reducedMotion = ref(false)

let abortCtrl: AbortController | null = null
let mobileMq: MediaQueryList | null = null
let motionMq: MediaQueryList | null = null

const hidden = computed(() =>
  HIDDEN_PREFIXES.some((p) => route.path === p || route.path.startsWith(`${p}/`))
)

const canSend = computed(() => input.value.trim().length > 0 && !sending.value)

function onMobileMq() {
  isMobile.value = mobileMq?.matches ?? false
}

function onMotionMq() {
  reducedMotion.value = motionMq?.matches ?? false
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function persist() {
  saveAiChatHistory(messages.value)
}

function toggleOpen() {
  if (!auth.isLoggedIn) {
    open.value = true
    loginHint.value = true
    return
  }
  loginHint.value = false
  open.value = !open.value
}

function closePanel() {
  open.value = false
}

function goLogin() {
  open.value = false
  void router.push({ path: '/login', query: { redirect: route.fullPath } })
}

function clearChat() {
  if (sending.value) return
  messages.value = []
  clearAiChatHistory()
  loginHint.value = false
  panelError.value = null
}

function htmlOf(content: string): string {
  return renderMarkdown(content)
}

async function onSend() {
  if (!auth.isLoggedIn) {
    loginHint.value = true
    open.value = true
    return
  }
  loginHint.value = false
  panelError.value = null
  const text = input.value.trim()
  if (!text || sending.value) return

  const userMsg: AiChatMessage = {
    id: newMessageId(),
    role: 'user',
    content: text,
    createdAt: Date.now()
  }
  messages.value.push(userMsg)
  input.value = ''
  persist()
  await scrollToBottom()

  const assistant: AiChatMessage = {
    id: newMessageId(),
    role: 'assistant',
    content: '',
    sources: [],
    createdAt: Date.now()
  }
  messages.value.push(assistant)
  sending.value = true
  abortCtrl = new AbortController()

  try {
    await askStream(
      text,
      (ev) => {
        const last = messages.value[messages.value.length - 1]
        if (!last || last.id !== assistant.id) return
        if (ev.type === 'token') {
          last.content += ev.text
          void scrollToBottom()
        } else if (ev.type === 'sources') {
          last.sources = ev.sources
        }
      },
      abortCtrl.signal
    )
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      // 离开页面：保留已收到的片段
    } else if (e instanceof AiAskError && (e.status === 503 || e.status === 429 || e.status === 401)) {
      messages.value = messages.value.filter((m) => m.id !== assistant.id)
      if (e.status === 503) panelError.value = 'down'
      else if (e.status === 429) panelError.value = 'limit'
      else loginHint.value = true
    } else {
      assistant.content =
        assistant.content || (e instanceof Error ? e.message : '请求失败，请稍后再试')
    }
  } finally {
    sending.value = false
    abortCtrl = null
    persist()
    await scrollToBottom()
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    void onSend()
  }
}

function sourceIsExternal(url: string): boolean {
  return /^https?:\/\//i.test(url)
}

function onSourceClick(s: AiSourceRef) {
  if (sourceIsExternal(s.url)) {
    window.open(s.url, '_blank', 'noopener,noreferrer')
  } else {
    open.value = false
    void router.push(s.url)
  }
}

watch(open, (v) => {
  if (v) void scrollToBottom()
})

onMounted(() => {
  messages.value = loadAiChatHistory()
  mobileMq = window.matchMedia(MOBILE_MQ)
  motionMq = window.matchMedia('(prefers-reduced-motion: reduce)')
  onMobileMq()
  onMotionMq()
  mobileMq.addEventListener('change', onMobileMq)
  motionMq.addEventListener('change', onMotionMq)
})

onUnmounted(() => {
  abortCtrl?.abort()
  mobileMq?.removeEventListener('change', onMobileMq)
  motionMq?.removeEventListener('change', onMotionMq)
})
</script>

<template>
  <div
    v-if="!hidden"
    class="ai-widget"
    :class="{
      'is-open': open,
      'is-mobile': isMobile,
      'is-reduced': reducedMotion
    }"
  >
    <!-- 面板 -->
    <div
      v-show="open"
      class="ai-panel"
      role="dialog"
      aria-label="极客栈 AI 助手"
      :aria-modal="isMobile ? 'true' : undefined"
    >
      <header class="ai-panel__head">
        <div class="ai-panel__titles">
          <span class="ai-panel__title font-display">AI 助手</span>
          <span class="ai-panel__sub">社区内容问答</span>
        </div>
        <div class="ai-panel__actions">
          <button
            type="button"
            class="ai-icon-btn"
            title="清空会话"
            :disabled="sending || messages.length === 0"
            @click="clearChat"
          >
            <n-icon :component="TrashOutline" :size="18" />
          </button>
          <button type="button" class="ai-icon-btn" title="关闭" @click="closePanel">
            <n-icon :component="CloseOutline" :size="20" />
          </button>
        </div>
      </header>

      <div v-if="loginHint && !auth.isLoggedIn" class="ai-login-hint">
        <p>登录后即可使用 AI 助手。</p>
        <GsButton variant="primary" @click="goLogin">去登录</GsButton>
      </div>

      <div ref="listRef" class="ai-list" aria-live="polite">
        <n-empty
          v-if="panelError === 'down'"
          class="ai-state"
          :description="AI_UNAVAILABLE"
        />
        <n-empty
          v-else-if="panelError === 'limit'"
          class="ai-state"
          :description="AI_RATE_LIMIT"
        />
        <template v-else>
          <p v-if="messages.length === 0 && !(loginHint && !auth.isLoggedIn)" class="ai-empty">
            问问社区里的帖子、Skill 或版规。
          </p>
          <div
            v-for="m in messages"
            :key="m.id"
            class="ai-msg"
            :class="m.role === 'user' ? 'ai-msg--user' : 'ai-msg--bot'"
          >
            <div v-if="m.role === 'user'" class="ai-msg__bubble">{{ m.content }}</div>
            <template v-else>
              <!-- eslint-disable vue/no-v-html -- 经 utils/markdown.ts DOMPurify 消毒（全站唯一出口） -->
              <div
                class="ai-msg__md"
                :class="{ 'is-streaming': sending && m.id === messages[messages.length - 1]?.id }"
                v-html="htmlOf(m.content || (sending ? '…' : ''))"
              ></div>
              <!-- eslint-enable vue/no-v-html -->
              <ul v-if="m.sources?.length" class="ai-sources">
                <li v-for="(s, i) in m.sources" :key="i">
                  <button type="button" class="ai-source" @click="onSourceClick(s)">
                    {{ s.title }}
                  </button>
                </li>
              </ul>
            </template>
          </div>
        </template>
      </div>

      <footer class="ai-panel__foot">
        <textarea
          v-model="input"
          class="ai-input"
          rows="2"
          maxlength="2000"
          :disabled="sending || !auth.isLoggedIn"
          placeholder="输入问题，Enter 发送"
          @keydown="onKeydown"
        />
        <GsButton
          variant="primary"
          class="ai-send"
          :disabled="!canSend || !auth.isLoggedIn"
          @click="onSend"
        >
          <n-icon :component="SendOutline" :size="18" />
          {{ sending ? '生成中…' : '发送' }}
        </GsButton>
      </footer>
    </div>

    <!-- 遮罩：仅移动端抽屉 -->
    <div
      v-show="open && isMobile"
      class="ai-scrim"
      aria-hidden="true"
      @click="closePanel"
    />

    <button
      type="button"
      class="ai-fab"
      :aria-expanded="open"
      aria-label="打开 AI 助手"
      @click="toggleOpen"
    >
      <n-icon :component="open ? CloseOutline : ChatbubbleEllipsesOutline" :size="24" />
    </button>
  </div>
</template>

<style scoped>
.ai-widget {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 40;
  pointer-events: none;
}

.ai-widget > * {
  pointer-events: auto;
}

.ai-fab {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-left: auto;
  border: 1px solid var(--border-strong);
  border-radius: 50%;
  background: var(--surface-2);
  color: var(--text-1);
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition:
    transform 0.2s var(--ease-out),
    background 0.15s var(--ease-out),
    border-color 0.15s var(--ease-out);
}

.ai-fab:hover {
  background: var(--surface-3);
  border-color: var(--border-strong);
  transform: translateY(-1px);
}

.ai-scrim {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: color-mix(in oklch, var(--ink-950) 45%, transparent);
}

.ai-panel {
  position: absolute;
  right: 0;
  bottom: calc(52px + 12px);
  z-index: 3;
  display: flex;
  flex-direction: column;
  width: min(380px, calc(100vw - 32px));
  height: min(520px, calc(100vh - 120px));
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  transform-origin: bottom right;
  animation: ai-panel-in 0.22s var(--ease-out);
}

.ai-widget.is-reduced .ai-panel {
  animation: none;
}

.ai-widget.is-reduced .ai-fab {
  transition: none;
}

@keyframes ai-panel-in {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.ai-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
}

.ai-panel__title {
  display: block;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text-1);
}

.ai-panel__sub {
  display: block;
  margin-top: 2px;
  font-size: 0.7rem;
  color: var(--text-3);
  font-family: var(--font-mono);
}

.ai-panel__actions {
  display: flex;
  gap: 4px;
}

.ai-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-small);
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
}

.ai-icon-btn:hover:not(:disabled) {
  color: var(--text-1);
  background: var(--surface-2);
}

.ai-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ai-login-hint {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
}

.ai-login-hint p {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-2);
}

.ai-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-empty {
  margin: auto 0;
  text-align: center;
  font-size: 0.8125rem;
  color: var(--text-3);
  line-height: 1.5;
  max-width: 28ch;
  align-self: center;
}

.ai-state {
  margin: auto 0;
  align-self: center;
}

.ai-msg--user {
  align-self: flex-end;
  max-width: 92%;
}

.ai-msg--bot {
  align-self: stretch;
  max-width: 100%;
}

.ai-msg__bubble {
  padding: 8px 12px;
  border-radius: var(--radius-small);
  background: var(--surface-3);
  color: var(--text-1);
  font-size: 0.875rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-msg__md {
  font-size: 0.875rem;
  line-height: 1.55;
  color: var(--text-1);
  word-break: break-word;
}

.ai-msg__md.is-streaming::after {
  content: '';
  display: inline-block;
  width: 0.45em;
  height: 1em;
  margin-left: 1px;
  vertical-align: text-bottom;
  background: var(--text-3);
  animation: ai-caret 0.9s steps(1) infinite;
}

.ai-widget.is-reduced .ai-msg__md.is-streaming::after {
  animation: none;
  opacity: 0.5;
}

@keyframes ai-caret {
  50% {
    opacity: 0;
  }
}

.ai-msg__md :deep(p) {
  margin: 0 0 0.55em;
}

.ai-msg__md :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-msg__md :deep(blockquote) {
  margin: 0.4em 0;
  padding-left: 0.75em;
  border-left: 2px solid var(--border-strong);
  color: var(--text-2);
}

.ai-msg__md :deep(code) {
  font-family: var(--font-mono);
  font-size: 0.85em;
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: var(--surface-2);
}

.ai-sources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
}

.ai-source {
  border: 1px solid var(--border);
  border-radius: var(--radius-small);
  background: var(--surface-2);
  color: var(--text-2);
  font-size: 0.75rem;
  padding: 4px 8px;
  cursor: pointer;
  font-family: inherit;
}

.ai-source:hover {
  color: var(--text-1);
  border-color: var(--border-strong);
}

.ai-panel__foot {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 10px 12px 12px;
  border-top: 1px solid var(--border);
  background: var(--surface-1);
}

.ai-input {
  flex: 1;
  resize: none;
  min-height: 44px;
  max-height: 96px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-small);
  background: var(--surface-2);
  color: var(--text-1);
  font-size: 0.875rem;
  font-family: inherit;
  line-height: 1.4;
}

.ai-input:focus {
  outline: 2px solid color-mix(in oklch, var(--accent) 35%, transparent);
  outline-offset: 1px;
  border-color: var(--border-strong);
}

.ai-input:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ai-send {
  flex-shrink: 0;
  min-height: 44px;
  padding-left: 14px;
  padding-right: 14px;
}

/* 移动端：全屏底部抽屉 */
.ai-widget.is-mobile {
  right: 16px;
  bottom: 16px;
}

.ai-widget.is-mobile .ai-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: min(92vh, 100%);
  max-height: 100%;
  border-radius: var(--radius) var(--radius) 0 0;
  animation: ai-drawer-in 0.24s var(--ease-out);
}

.ai-widget.is-mobile.is-reduced .ai-panel {
  animation: none;
}

@keyframes ai-drawer-in {
  from {
    transform: translateY(24px);
    opacity: 0.85;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.ai-widget.is-mobile .ai-fab {
  position: fixed;
  right: 16px;
  bottom: 16px;
}
</style>

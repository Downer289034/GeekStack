<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'

export interface HeroTypingSegment {
  text: string
  em?: boolean
  br?: boolean
}

interface Piece {
  em: boolean
  text: string
  br?: boolean
}

const props = withDefaults(
  defineProps<{
    segments: HeroTypingSegment[]
    ariaLabel?: string
    startDelay?: number
    charMs?: number
    brPauseMs?: number
    segPauseMs?: number
    replayLabel?: string
  }>(),
  {
    ariaLabel: undefined,
    startDelay: 400,
    charMs: 165,
    brPauseMs: 220,
    segPauseMs: 60,
    replayLabel: '重播动画'
  }
)

const pieces = ref<Piece[]>([])
const caretVisible = ref(false)
const reduced = ref(false)
const playing = ref(false)

let timer: ReturnType<typeof setTimeout> | null = null
let runId = 0
let media: MediaQueryList | null = null

const fullLabel = computed(() => {
  if (props.ariaLabel) return props.ariaLabel
  return props.segments.map((s) => s.text).join('')
})

function stop() {
  runId += 1
  if (timer != null) {
    clearTimeout(timer)
    timer = null
  }
}

function schedule(fn: () => void, ms: number, id: number) {
  timer = setTimeout(() => {
    if (id !== runId) return
    fn()
  }, ms)
}

function renderFinal(hideCaret: boolean) {
  stop()
  playing.value = false
  pieces.value = props.segments.map((s) => ({
    em: Boolean(s.em),
    text: s.text,
    br: Boolean(s.br)
  }))
  caretVisible.value = !hideCaret
}

function type() {
  stop()
  const id = runId
  playing.value = true
  pieces.value = []
  caretVisible.value = true

  let si = 0
  let ci = 0

  const tick = () => {
    if (id !== runId) return
    if (si >= props.segments.length) {
      /* 打完保留全文，光标继续闪，不再清空重打 */
      playing.value = true
      return
    }

    const s = props.segments[si]
    if (ci === 0) {
      pieces.value = [...pieces.value, { em: Boolean(s.em), text: '', br: false }]
    }

    const idx = pieces.value.length - 1
    const copy = pieces.value.slice()
    copy[idx] = {
      ...copy[idx],
      text: s.text.slice(0, ++ci),
      br: ci >= s.text.length ? Boolean(s.br) : false
    }
    pieces.value = copy

    if (ci >= s.text.length) {
      si++
      ci = 0
      schedule(tick, s.br ? props.brPauseMs : props.segPauseMs, id)
      return
    }
    schedule(tick, props.charMs, id)
  }

  schedule(tick, props.startDelay, id)
}

/** 评审 / 调手感：?motion=force 首屏仍自动打字 */
function shouldForceMotion(): boolean {
  try {
    return new URLSearchParams(window.location.search).get('motion') === 'force'
  } catch {
    return false
  }
}

function replay() {
  type()
}

function applyMotionPreference() {
  const force = shouldForceMotion()
  reduced.value = Boolean(media?.matches) && !force
  if (reduced.value) {
    renderFinal(true)
  } else {
    type()
  }
}

defineExpose({ replay })

onMounted(async () => {
  await nextTick()
  media = window.matchMedia('(prefers-reduced-motion: reduce)')
  applyMotionPreference()
  media.addEventListener('change', applyMotionPreference)
})

onUnmounted(() => {
  stop()
  media?.removeEventListener('change', applyMotionPreference)
})
</script>

<template>
  <div class="hero-typing-wrap" :class="{ 'is-playing': playing }">
    <h1 class="hero-typing font-display" :aria-label="fullLabel">
      <span class="hero-typing__zone" aria-hidden="true">
        <template v-for="(p, i) in pieces" :key="i">
          <em v-if="p.em">{{ p.text }}</em>
          <span v-else>{{ p.text }}</span>
          <br v-if="p.br" />
        </template>
        <span
          v-show="caretVisible"
          class="hero-typing__caret"
          aria-hidden="true"
        />
      </span>
    </h1>
    <button
      type="button"
      class="hero-typing__replay"
      :aria-label="replayLabel"
      @click="replay"
    >
      {{ replayLabel }}
    </button>
  </div>
</template>

<style scoped>
.hero-typing-wrap {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.hero-typing {

.hero-typing {
  margin: 0;
  font-size: clamp(2.2rem, 4.5vw + 0.6rem, 3.6rem);
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.015em;
  max-width: 18em;
  min-height: 2.36em;
  color: var(--text-1);
}

.hero-typing em {
  font-style: normal;
  color: var(--accent);
}

.hero-typing__caret {
  display: inline-block;
  width: 3px;
  height: 0.85em;
  background: var(--accent);
  margin-left: 5px;
  vertical-align: -0.06em;
  border-radius: 1px;
  animation: hero-caret-blink 1s steps(1) infinite;
}

@keyframes hero-caret-blink {
  50% {
    opacity: 0;
  }
}

.hero-typing__replay {
  align-self: flex-end;
  margin-top: 8px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-3);
  border-radius: var(--radius-small);
  padding: 4px 10px;
  min-height: 44px;
  font-size: 0.72rem;
  font-family: inherit;
  cursor: pointer;
  opacity: 0.72;
  transition:
    color 0.15s var(--ease-out),
    border-color 0.15s var(--ease-out),
    opacity 0.15s var(--ease-out);
}

.hero-typing__replay:hover,
.hero-typing__replay:focus-visible {
  color: var(--accent);
  border-color: var(--accent);
  opacity: 1;
}

@media (prefers-reduced-motion: reduce) {
  .hero-typing-wrap:not(.is-playing) .hero-typing__caret {
    display: none !important;
    animation: none !important;
  }

  /* 用户主动点「重播」时允许光标闪（入口，不是无视偏好） */
  .hero-typing-wrap.is-playing .hero-typing__caret {
    display: inline-block !important;
    animation: hero-caret-blink 1s steps(1) infinite !important;
  }
}
</style>

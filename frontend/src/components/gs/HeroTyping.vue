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
    caretHoldMs?: number
    /** 打完并收起光标后，间隔多久再重播 */
    loopGapMs?: number
  }>(),
  {
    ariaLabel: undefined,
    startDelay: 400,
    charMs: 165,
    brPauseMs: 220,
    segPauseMs: 60,
    caretHoldMs: 2400,
    loopGapMs: 3000
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
      /* 打完：光标停留片刻后熄灭，loopGapMs 后自动重播（老板口径：每 3 秒自动循环） */
      playing.value = true
      schedule(() => {
        caretVisible.value = false
        schedule(() => {
          type()
        }, props.loopGapMs, id)
      }, props.caretHoldMs, id)
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

function applyMotionPreference() {
  const force = shouldForceMotion()
  reduced.value = Boolean(media?.matches) && !force
  if (reduced.value) {
    renderFinal(true)
  } else {
    type()
  }
}

defineExpose({ replay: type })

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
  </div>
</template>

<style scoped>
.hero-typing-wrap {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

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

@media (prefers-reduced-motion: reduce) {
  .hero-typing-wrap:not(.is-playing) .hero-typing__caret {
    display: none !important;
    animation: none !important;
  }
}
</style>

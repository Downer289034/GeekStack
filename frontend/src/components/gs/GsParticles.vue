<script setup lang="ts">
// T-DESIGN-P1：全站背景粒子氛围层（规格：docs/geekstack-particles-bg.html v1.0 §集成规格）
// 红线：纯装饰不交互（禁跟随/连线）、canvas fixed 底层只在留白可见、"被用户注意到即失败"。
// 门控：prefers-reduced-motion 不启动；≤768px 不启动；/login 排除（登录页 LoginBackground 自带粒子，两套不重叠）。
// 性能四件套：DPR 封顶 2、visibilitychange 暂停、掉帧降级（rAF delta >33ms 连续 60 帧 → 密度减半，再触发 → 停用）、App 级单实例。
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { storeToRefs } from 'pinia'

const EXCLUDED_PATHS = new Set(['/login'])
const EXCLUDED_PREFIXES = ['/oauth/callback']
const MAX_DENSITY = 56
const AREA_PER_PARTICLE = 26000
const MAX_DPR = 2
const SLOW_MS = 33
const SLOW_FRAMES_LIMIT = 60
const MIN_POOL = 8

interface Particle {
  x: number
  y: number
  r: number
  vy: number
  drift: number
  ph: number
  /** 0–1 归一化基准透明度，绘制时映射到当前主题 alpha 区间（主题切换即时生效） */
  baseA: number
}

const route = useRoute()
const { theme } = storeToRefs(useThemeStore())

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
const smallScreen = window.matchMedia('(max-width: 768px)')

const enabled = ref(false)
const canvasRef = ref<HTMLCanvasElement | null>(null)

let ctx: CanvasRenderingContext2D | null = null
let parts: Particle[] = []
let rafId = 0
let running = false
let W = 0
let H = 0
let rgb: [number, number, number] = [255, 186, 100]
let aMin = 0.08
let aMax = 0.28
let lastT = 0
let slowFrames = 0
let degraded = false
let cleanupFns: Array<() => void> = []

function isExcluded(path: string): boolean {
  return EXCLUDED_PATHS.has(path) || EXCLUDED_PREFIXES.some((p) => path.startsWith(p))
}

/** 调色取自 tokens.css 的 --particles-rgb（夜/日两套，主题切换即时生效）；alpha 区间出处 particles-bg.html §2 */
function readPalette(): void {
  const raw = getComputedStyle(document.documentElement).getPropertyValue('--particles-rgb').trim()
  const parsed = raw.split(',').map((n) => Number.parseInt(n, 10))
  if (parsed.length === 3 && parsed.every((n) => Number.isFinite(n))) {
    rgb = [parsed[0], parsed[1], parsed[2]]
  }
  const light = document.documentElement.dataset.theme === 'light'
  aMin = light ? 0.05 : 0.08
  aMax = light ? 0.14 : 0.28
}

function spawn(fromBottom: boolean): Particle {
  return {
    x: Math.random() * W,
    y: fromBottom ? H + 8 : Math.random() * H,
    r: 0.8 + Math.random() * 1.6,
    vy: 0.1 + Math.random() * 0.26,
    drift: 0.04 + Math.random() * 0.07,
    ph: Math.random() * Math.PI * 2,
    baseA: Math.random()
  }
}

function targetCount(): number {
  return Math.min(MAX_DENSITY, Math.round((W * H) / AREA_PER_PARTICLE))
}

function resize(): void {
  const cv = canvasRef.value
  if (!cv || !ctx) return
  const dpr = Math.min(window.devicePixelRatio || 1, MAX_DPR)
  W = window.innerWidth
  H = window.innerHeight
  cv.width = W * dpr
  cv.height = H * dpr
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  parts = Array.from({ length: targetCount() }, () => spawn(false))
}

function tick(t: number): void {
  if (!running) return
  // 掉帧降级：暂停恢复后 lastT 会重置，不会误计恢复首帧
  if (lastT > 0 && t - lastT > SLOW_MS) {
    slowFrames += 1
    if (slowFrames >= SLOW_FRAMES_LIMIT) {
      if (degraded || parts.length <= MIN_POOL * 2) {
        stop('掉帧降级触发两次，粒子层停用')
        return
      }
      parts.length = Math.max(MIN_POOL, Math.floor(parts.length / 2))
      degraded = true
      slowFrames = 0
      console.warn('[GsParticles] 掉帧降级：密度减半至', parts.length)
    }
  } else {
    slowFrames = 0
  }
  lastT = t

  ctx!.clearRect(0, 0, W, H)
  const [r, g, b] = rgb
  for (let i = 0; i < parts.length; i++) {
    const p = parts[i]
    p.y -= p.vy
    p.x += p.drift + Math.sin(t / 2400 + p.ph) * 0.1
    const alpha = aMin + p.baseA * (aMax - aMin)
    const tw = alpha * (0.6 + 0.4 * Math.sin(t / 900 + p.ph * 3))
    ctx!.beginPath()
    ctx!.fillStyle = `rgba(${r},${g},${b},${tw.toFixed(3)})`
    ctx!.arc(p.x, p.y, p.r, 0, 7)
    ctx!.fill()
    if (p.y < -10 || p.x < -12 || p.x > W + 12) {
      parts[i] = spawn(true)
    }
  }
  rafId = requestAnimationFrame(tick)
}

function start(): void {
  if (running) return
  running = true
  lastT = 0
  rafId = requestAnimationFrame(tick)
}

function stop(reason: string): void {
  running = false
  if (rafId) cancelAnimationFrame(rafId)
  rafId = 0
  if (reason) console.info('[GsParticles] 停用：', reason)
}

function teardown(): void {
  stop('')
  cleanupFns.forEach((fn) => fn())
  cleanupFns = []
  ctx = null
  parts = []
}

function init(cv: HTMLCanvasElement): void {
  ctx = cv.getContext('2d')
  if (!ctx) {
    enabled.value = false
    return
  }
  readPalette()
  resize()
  const onResize = () => resize()
  const onVisibility = () => {
    if (document.hidden) {
      stop('')
    } else {
      start()
    }
  }
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVisibility)
  cleanupFns = [
    () => window.removeEventListener('resize', onResize),
    () => document.removeEventListener('visibilitychange', onVisibility)
  ]
  start()
}

function refresh(): void {
  enabled.value = !reducedMotion.matches && !smallScreen.matches && !isExcluded(route.path)
}

watch(
  enabled,
  (on) => {
    teardown()
    if (on) {
      void nextTick(() => {
        if (canvasRef.value && enabled.value) init(canvasRef.value)
      })
    }
  },
  { immediate: true }
)

watch(
  [() => route.path, theme],
  () => {
    refresh()
    readPalette()
  }
)

function onMotionChange(): void {
  refresh()
}
reducedMotion.addEventListener('change', onMotionChange)
smallScreen.addEventListener('change', onMotionChange)
cleanupFns.push(() => {
  reducedMotion.removeEventListener('change', onMotionChange)
  smallScreen.removeEventListener('change', onMotionChange)
})

onBeforeUnmount(teardown)

refresh()
</script>

<template>
  <canvas v-if="enabled" ref="canvasRef" class="gs-particles" aria-hidden="true"></canvas>
</template>

<style scoped>
.gs-particles {
  position: fixed;
  inset: 0;
  z-index: -1;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>

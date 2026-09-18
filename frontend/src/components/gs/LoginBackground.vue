<script setup lang="ts">
/**
 * T-LOGIN-BG：登录页实景视差背景（handoff v1.0 / v2.3.2）
 * 三件：视差壳 img + 暗角 + 萤火粒子。与表单解耦；仅登录路由挂载。
 * App 级 GsParticles 已排除 /login——本画布独立，两套不重叠。
 */
import { onMounted, onUnmounted, ref } from 'vue'
import duskUrl from '@docs/assets/login-bg-dusk.jpg'

const MAX_PARTICLES = 44
const AREA_PER = 34000
const MAX_DPR = 2
const LERP = 0.05
const FPS_WINDOW = 90
const FPS_FLOOR = 50

interface Particle {
  x: number
  y: number
  r: number
  vy: number
  drift: number
  ph: number
  baseA: number
}

const parRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const imgFailed = ref(false)
const isStaticShell = ref(true)

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
const finePointer = window.matchMedia('(pointer: fine)')

let rafId = 0
let running = false
let interactive = false
let staticMode = false
let particleHalf = false

let tx = 0
let ty = 0
let cx = 0
let cy = 0

let ctx: CanvasRenderingContext2D | null = null
let parts: Particle[] = []
let W = 0
let H = 0
let lastT = 0
const dts: number[] = []
let rgb: [number, number, number] = [255, 186, 100]

function readRgb(): void {
  const raw = getComputedStyle(document.documentElement).getPropertyValue('--particles-rgb').trim()
  const parsed = raw.split(',').map((n) => Number.parseInt(n, 10))
  if (parsed.length === 3 && parsed.every((n) => Number.isFinite(n))) {
    rgb = [parsed[0], parsed[1], parsed[2]]
  }
}

function targetCount(): number {
  const n = Math.min(MAX_PARTICLES, Math.round((W * H) / AREA_PER) || 0)
  return particleHalf ? Math.max(8, Math.floor(n / 2)) : n
}

function spawn(fromBottom: boolean): Particle {
  return {
    x: Math.random() * W,
    y: fromBottom ? H + 8 : Math.random() * H,
    r: 0.8 + Math.random() * 1.4,
    vy: 0.1 + Math.random() * 0.25,
    drift: 0.04 + Math.random() * 0.07,
    ph: Math.random() * Math.PI * 2,
    baseA: Math.random()
  }
}

function resizeCanvas(): void {
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

function applyParTransform(): void {
  const el = parRef.value
  if (!el) return
  if (!interactive || staticMode) {
    el.style.transform = 'translate3d(0,0,0) scale(1.2)'
    return
  }
  const sy = Math.min(window.scrollY, window.innerHeight)
  const x = (-cx * 20).toFixed(2)
  const y = (-cy * 14 - sy * 0.05).toFixed(2)
  el.style.transform = `translate3d(${x}px,${y}px,0) scale(1.2)`
}

function drawParticles(t: number): void {
  if (!ctx || parts.length === 0) return
  ctx.clearRect(0, 0, W, H)
  const aMin = 0.06
  const aMax = 0.22
  const [r, g, b] = rgb
  for (let i = 0; i < parts.length; i++) {
    const p = parts[i]
    p.y -= p.vy
    p.x += p.drift + Math.sin(t / 2400 + p.ph) * 0.1
    const alpha = aMin + p.baseA * (aMax - aMin)
    const tw = alpha * (0.6 + 0.4 * Math.sin(t / 900 + p.ph * 3))
    ctx.beginPath()
    ctx.fillStyle = `rgba(${r},${g},${b},${tw.toFixed(3)})`
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fill()
    if (p.y < -10 || p.x < -12 || p.x > W + 12) {
      parts[i] = spawn(true)
    }
  }
}

function checkFps(dt: number): void {
  if (!(dt > 0) || staticMode) return
  dts.push(dt)
  if (dts.length > FPS_WINDOW) dts.shift()
  if (dts.length < FPS_WINDOW) return
  const avgDt = dts.reduce((a, b) => a + b, 0) / dts.length
  const avgFps = 1000 / avgDt
  if (avgFps < FPS_FLOOR) {
    staticMode = true
    particleHalf = true
    isStaticShell.value = true
    parts.length = Math.max(8, Math.floor(parts.length / 2))
    applyParTransform()
    console.info('[LoginBackground] 掉帧降级：静态视差 + 粒子减半')
  }
}

function tick(t: number): void {
  if (!running) return
  const dt = lastT > 0 ? t - lastT : 16
  lastT = t
  checkFps(dt)

  if (interactive && !staticMode) {
    cx += (tx - cx) * LERP
    cy += (ty - cy) * LERP
    applyParTransform()
  }

  if (!reducedMotion.matches) {
    drawParticles(t)
  }

  rafId = requestAnimationFrame(tick)
}

function onMove(e: MouseEvent): void {
  tx = (e.clientX / window.innerWidth) * 2 - 1
  ty = (e.clientY / window.innerHeight) * 2 - 1
}

function onResize(): void {
  resizeCanvas()
  applyParTransform()
}

function onVisibility(): void {
  if (document.hidden) {
    running = false
    if (rafId) cancelAnimationFrame(rafId)
    rafId = 0
  } else if (!running) {
    running = true
    lastT = 0
    rafId = requestAnimationFrame(tick)
  }
}

function onImgError(): void {
  imgFailed.value = true
}

function onImgLoad(): void {
  imgFailed.value = false
}

onMounted(() => {
  interactive = !reducedMotion.matches && finePointer.matches
  isStaticShell.value = !interactive
  readRgb()
  applyParTransform()

  const cv = canvasRef.value
  if (cv && !reducedMotion.matches) {
    ctx = cv.getContext('2d')
    if (ctx) resizeCanvas()
  }

  if (interactive) {
    window.addEventListener('mousemove', onMove, { passive: true })
  }
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVisibility)

  running = true
  lastT = 0
  rafId = requestAnimationFrame(tick)
})

onUnmounted(() => {
  running = false
  if (rafId) cancelAnimationFrame(rafId)
  rafId = 0
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibility)
  ctx = null
  parts = []
})
</script>

<template>
  <div
    class="login-bg"
    :class="{ 'is-failed': imgFailed, 'is-static': isStaticShell }"
    aria-hidden="true"
  >
    <div ref="parRef" class="login-bg__par">
      <img
        class="login-bg__img"
        :src="duskUrl"
        alt=""
        decoding="async"
        fetchpriority="high"
        @error="onImgError"
        @load="onImgLoad"
      />
    </div>
    <div class="login-bg__vignette" />
    <canvas ref="canvasRef" class="login-bg__fx" />
  </div>
</template>

<style scoped>
.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: var(--ink-950);
  pointer-events: none;
}

.login-bg__par {
  position: absolute;
  inset: 0;
  will-change: transform;
  transform: translate3d(0, 0, 0) scale(1.2);
}

.login-bg__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 80%;
  display: block;
}

.login-bg.is-failed .login-bg__img {
  opacity: 0;
}

/* 暗角：只压四边，禁全屏 scrim */
.login-bg__vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(
      ellipse 120% 90% at 50% 46%,
      transparent 58%,
      color-mix(in oklch, var(--ink-950) 38%, transparent) 100%
    ),
    linear-gradient(to top, color-mix(in oklch, var(--ink-950) 30%, transparent), transparent 26%);
}

.login-bg__fx {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.login-bg.is-static .login-bg__par {
  transform: translate3d(0, 0, 0) scale(1.2) !important;
}
</style>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  NButton,
  NCheckbox,
  NForm,
  NFormItem,
  NInput,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { register } from '@/api/auth'
import { beginOAuth } from '@/api/oauth'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import type { GsTheme } from '@/theme/tokens'
import loginBgUrl from '@/assets/login-bg.mp4'

const router = useRouter()
const auth = useAuthStore()
const themeStore = useThemeStore()

const tab = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInst | null>(null)
const registerFormRef = ref<FormInst | null>(null)
const submitting = ref(false)
const rememberMe = ref(false)

/** 是否允许插入 video（降级矩阵） */
const allowVideo = ref(false)
/** 运行时失败后强制降级 */
const videoFailed = ref(false)
const videoRef = ref<HTMLVideoElement | null>(null)

let prevTheme: GsTheme | null = null

function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function isSaveData(): boolean {
  const nav = navigator as Navigator & {
    connection?: { saveData?: boolean; effectiveType?: string }
  }
  const c = nav.connection
  if (c?.saveData) return true
  const t = c?.effectiveType
  return t === 'slow-2g' || t === '2g'
}

function isMobileViewport(): boolean {
  return window.matchMedia('(max-width: 768px)').matches
}

function resolveAllowVideo() {
  if (prefersReducedMotion() || isSaveData() || isMobileViewport()) {
    allowVideo.value = false
    return
  }
  allowVideo.value = true
}

const showVideo = computed(() => allowVideo.value && !videoFailed.value)

function onVideoError() {
  videoFailed.value = true
}

const loginModel = reactive({ username: '', password: '' })
const registerModel = reactive({ username: '', email: '', password: '' })

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名' },
    { min: 3, max: 50, message: '用户名长度需在 3-50 位之间' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字与下划线' }
  ],
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '邮箱格式不正确' }
  ],
  password: [
    { required: true, message: '请输入密码' },
    { min: 8, max: 64, message: '密码长度需在 8-64 位之间' }
  ]
}

function goHome() {
  const redirect = router.currentRoute.value.query.redirect
  void router.push(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/')
}

async function onLoginSubmit() {
  try {
    await loginFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await auth.login(loginModel.username, loginModel.password)
    goHome()
  } catch {
    // toast by interceptor
  } finally {
    submitting.value = false
  }
}

async function onRegisterSubmit() {
  try {
    await registerFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const pair = await register({ ...registerModel })
    await auth.applyTokens(pair)
    goHome()
  } catch {
    // toast by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  /* 登录页锁定夜色（视频成立前提）；离开时恢复 */
  prevTheme = themeStore.theme
  if (themeStore.theme !== 'dark') {
    themeStore.setTheme('dark')
  }
  resolveAllowVideo()
})

onUnmounted(() => {
  if (prevTheme && prevTheme !== themeStore.theme) {
    themeStore.setTheme(prevTheme)
  }
  /* 路由离开：video 随 v-if 销毁，无持续请求 */
})
</script>

<template>
  <div class="login-page">
    <div class="login-bg" :class="{ 'is-fallback': !showVideo }" aria-hidden="true">
      <video
        v-if="showVideo"
        ref="videoRef"
        class="login-bg__video"
        :src="loginBgUrl"
        autoplay
        muted
        loop
        playsinline
        disablepictureinpicture
        preload="metadata"
        tabindex="-1"
        aria-hidden="true"
        @error="onVideoError"
      />
    </div>

    <main class="login-stage">
      <div class="login-card">
        <div class="login-brand">
          <span class="login-brand__mark" aria-hidden="true">极</span>
          <div>
            <div class="login-brand__name">极客栈</div>
            <div class="login-brand__slogan font-mono">代码为灯，技术为栈</div>
          </div>
        </div>

        <div class="login-tabs" role="tablist" aria-label="登录或注册">
          <button
            type="button"
            role="tab"
            :aria-selected="tab === 'login'"
            @click="tab = 'login'"
          >
            登录
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="tab === 'register'"
            @click="tab = 'register'"
          >
            注册
          </button>
        </div>

        <div v-show="tab === 'login'" class="login-body" role="tabpanel">
          <n-form ref="loginFormRef" :model="loginModel" :rules="rules" label-placement="top">
            <n-form-item label="用户名" path="username">
              <n-input v-model:value="loginModel.username" placeholder="用户名" />
            </n-form-item>
            <n-form-item label="密码" path="password">
              <n-input
                v-model:value="loginModel.password"
                type="password"
                show-password-on="click"
                placeholder="密码"
                @keyup.enter="onLoginSubmit"
              />
            </n-form-item>
            <div class="login-row">
              <n-checkbox v-model:checked="rememberMe">记住我</n-checkbox>
              <span class="login-forgot">忘记密码？</span>
            </div>
            <n-button type="primary" block :loading="submitting" @click="onLoginSubmit">
              登录
            </n-button>
          </n-form>
        </div>

        <div v-show="tab === 'register'" class="login-body" role="tabpanel">
          <n-form
            ref="registerFormRef"
            :model="registerModel"
            :rules="rules"
            label-placement="top"
          >
            <n-form-item label="用户名" path="username">
              <n-input
                v-model:value="registerModel.username"
                placeholder="3-50 位字母、数字或下划线"
              />
            </n-form-item>
            <n-form-item label="邮箱" path="email">
              <n-input v-model:value="registerModel.email" placeholder="邮箱" />
            </n-form-item>
            <n-form-item label="密码" path="password">
              <n-input
                v-model:value="registerModel.password"
                type="password"
                show-password-on="click"
                placeholder="8-64 位"
              />
            </n-form-item>
            <n-button type="primary" block :loading="submitting" @click="onRegisterSubmit">
              注册并登录
            </n-button>
          </n-form>
        </div>

        <div class="login-divider" role="separator">第三方登录</div>
        <div class="login-oauth">
          <button type="button" class="login-oauth__btn" @click="beginOAuth('GITHUB')">
            使用 GitHub 登录
          </button>
          <button type="button" class="login-oauth__btn" @click="beginOAuth('GITEE')">
            使用 Gitee 登录
          </button>
        </div>

        <RouterLink class="login-back" to="/">← 返回首页</RouterLink>
      </div>
    </main>

    <p class="login-copy">© 2026 极客栈 GeekStack</p>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  color: var(--text-1);
  /* 卡片实色不透明，任意视频帧上表单对比度由卡片自担（无视频 scrim） */
  --login-card-bg: oklch(20% 0.02 262);
}

.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: var(--ink-950, oklch(13% 0.015 262));
  pointer-events: none;
}

.login-bg__video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 降级：墨蓝 + 48px 微网格，无 scrim */
.login-bg.is-fallback::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent 0 47px,
      oklch(25% 0.022 262 / 0.25) 47px 48px
    ),
    repeating-linear-gradient(
      90deg,
      transparent 0 47px,
      oklch(25% 0.022 262 / 0.25) 47px 48px
    ),
    var(--ink-950, oklch(13% 0.015 262));
}

.login-stage {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 48px 8vw 48px 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--login-card-bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 36px 32px 28px;
  /* 禁 backdrop-blur */
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.login-brand__mark {
  width: 40px;
  height: 40px;
  border-radius: 9px;
  flex-shrink: 0;
  background: var(--accent);
  color: var(--accent-text);
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.25rem;
}

.login-brand__name {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.125rem;
}

.login-brand__slogan {
  margin-top: 2px;
  font-size: 0.75rem;
  color: var(--text-3);
}

.login-tabs {
  display: flex;
  border-bottom: 1px solid var(--border);
  margin-bottom: 24px;
}

.login-tabs button {
  flex: 1;
  border: none;
  background: none;
  padding: 10px 0;
  min-height: 44px;
  font-size: 0.875rem;
  color: var(--text-3);
  cursor: pointer;
  font-family: inherit;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition:
    color 0.15s var(--ease-out),
    border-color 0.15s var(--ease-out);
}

.login-tabs button:hover {
  color: var(--text-2);
}

.login-tabs button[aria-selected='true'] {
  color: var(--text-1);
  font-weight: 600;
  border-bottom-color: var(--accent);
}

.login-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 琥珀纪律：忘记密码中性，不占强调色额度 */
.login-forgot {
  font-size: 0.8125rem;
  color: var(--text-3);
}

.login-divider {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 22px 0 16px;
  color: var(--text-3);
  font-size: 0.75rem;
}

.login-divider::before,
.login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.login-oauth {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.login-oauth__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 40px;
  padding: 8px 12px;
  text-decoration: none;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--text-2);
  font-size: 0.8125rem;
  font-family: inherit;
  cursor: pointer;
  transition:
    border-color 0.15s var(--ease-out),
    color 0.15s var(--ease-out);
}

.login-oauth__btn:hover {
  border-color: var(--border-strong);
  color: var(--text-1);
}

.login-back {
  display: block;
  margin-top: 18px;
  text-align: center;
  font-size: 0.8125rem;
  color: var(--text-3);
  text-decoration: none;
}

.login-back:hover {
  color: var(--text-2);
}

.login-copy {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 14px;
  z-index: 1;
  margin: 0;
  text-align: center;
  font-size: 0.75rem;
  color: color-mix(in oklch, var(--text-3) 80%, transparent);
  pointer-events: none;
}

/* Naive 输入框焦点：琥珀边 + 14% 光环（本页覆盖） */
.login-card :deep(.n-input:not(.n-input--disabled).n-input--focus) {
  --n-border-hover: var(--accent) !important;
  --n-border-focus: var(--accent) !important;
  --n-box-shadow-focus: 0 0 0 3px color-mix(in oklch, var(--accent) 14%, transparent) !important;
}

@media (max-width: 900px) {
  .login-stage {
    justify-content: center;
    padding: 48px 20px;
  }
}

@media (max-width: 560px) {
  .login-card {
    padding: 28px 22px 22px;
  }

  .login-copy {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-tabs button,
  .login-oauth__btn,
  .login-back {
    transition: none !important;
  }
}
</style>

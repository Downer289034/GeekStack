<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
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
import LoginBackground from '@/components/gs/LoginBackground.vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import type { GsTheme } from '@/theme/tokens'

const router = useRouter()
const auth = useAuthStore()
const themeStore = useThemeStore()

const tab = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInst | null>(null)
const registerFormRef = ref<FormInst | null>(null)
const submitting = ref(false)
const rememberMe = ref(false)

let prevTheme: GsTheme | null = null

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
  /* 登录页锁定夜色（实景图成立前提）；离开时恢复 */
  prevTheme = themeStore.theme
  if (themeStore.theme !== 'dark') {
    themeStore.setTheme('dark')
  }
})

onUnmounted(() => {
  if (prevTheme && prevTheme !== themeStore.theme) {
    themeStore.setTheme(prevTheme)
  }
})
</script>

<template>
  <div class="login-page">
    <LoginBackground />

    <main class="login-stage">
      <div class="login-card">
        <!-- 图中已有「极客栈」品牌字：卡片内禁重复 HTML 字标（handoff §4） -->
        <div class="login-tabs" role="tablist" aria-label="登录或注册">
          <button
            type="button"
            role="tab"
            :aria-selected="tab === 'login'"
            :tabindex="tab === 'login' ? 0 : -1"
            @click="tab = 'login'"
          >
            登录
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="tab === 'register'"
            :tabindex="tab === 'register' ? 0 : -1"
            @click="tab = 'register'"
          >
            注册
          </button>
        </div>

        <div v-show="tab === 'login'">
          <n-form
            ref="loginFormRef"
            :model="loginModel"
            :rules="rules"
            label-placement="top"
            require-mark-placement="right-hanging"
          >
            <n-form-item label="用户名" path="username">
              <n-input
                v-model:value="loginModel.username"
                placeholder="用户名"
                autocomplete="username"
              />
            </n-form-item>
            <n-form-item label="密码" path="password">
              <n-input
                v-model:value="loginModel.password"
                type="password"
                show-password-on="click"
                placeholder="密码"
                autocomplete="current-password"
              />
            </n-form-item>
            <div class="login-row">
              <n-checkbox v-model:checked="rememberMe">记住我</n-checkbox>
              <span class="login-forgot">忘记密码?</span>
            </div>
            <n-button type="primary" block :loading="submitting" @click="onLoginSubmit">
              登录
            </n-button>
          </n-form>
        </div>

        <div v-show="tab === 'register'">
          <n-form
            ref="registerFormRef"
            :model="registerModel"
            :rules="rules"
            label-placement="top"
            require-mark-placement="right-hanging"
          >
            <n-form-item label="用户名" path="username">
              <n-input v-model:value="registerModel.username" placeholder="用户名" />
            </n-form-item>
            <n-form-item label="邮箱" path="email">
              <n-input v-model:value="registerModel.email" placeholder="邮箱" />
            </n-form-item>
            <n-form-item label="密码" path="password">
              <n-input
                v-model:value="registerModel.password"
                type="password"
                show-password-on="click"
                placeholder="至少 8 位"
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
  /* 卡片 90% 实色 + 投影托层次（禁 backdrop-blur） */
  --login-card-bg: color-mix(in oklch, var(--ink-950) 90%, transparent);
}

.login-stage {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 40px 8vw 40px 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  /* 下移避让图中品牌字（handoff §2.1） */
  margin-top: 20vh;
  background: var(--login-card-bg);
  border: 1px solid color-mix(in oklch, var(--border) 80%, transparent);
  border-radius: 12px;
  padding: 36px 32px 28px;
  box-shadow: 0 24px 60px color-mix(in oklch, var(--ink-950) 50%, transparent);
  /* 禁 backdrop-blur */
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

  .login-card {
    margin-top: 8vh;
  }
}

@media (max-width: 560px) {
  .login-card {
    padding: 28px 22px 22px;
    margin-top: 4vh;
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

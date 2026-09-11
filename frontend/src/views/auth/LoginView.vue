<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  NButton,
  NCheckbox,
  NDivider,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NText,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { register } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const tab = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInst | null>(null)
const registerFormRef = ref<FormInst | null>(null)
const submitting = ref(false)
const rememberMe = ref(false)

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
    // 错误提示由请求拦截器统一 toast
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
    // 错误提示由请求拦截器统一 toast
  } finally {
    submitting.value = false
  }
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
</script>

<template>
  <div class="login-page">
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
            <n-text depth="3" class="login-forgot">忘记密码？</n-text>
          </div>
          <n-button type="primary" block :loading="submitting" @click="onLoginSubmit">登录</n-button>
        </n-form>
      </div>

      <div v-show="tab === 'register'" class="login-body" role="tabpanel">
        <n-form ref="registerFormRef" :model="registerModel" :rules="rules" label-placement="top">
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="registerModel.username" placeholder="3-50 位字母、数字或下划线" />
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

      <n-divider class="oauth-divider">
        <n-text depth="3" style="font-size: 12px">第三方登录</n-text>
      </n-divider>
      <n-space vertical size="small">
        <n-button block secondary tag="a" href="/api/v1/oauth/login/GITHUB">使用 GitHub 登录</n-button>
        <n-button block secondary tag="a" href="/api/v1/oauth/login/GITEE">使用 Gitee 登录</n-button>
      </n-space>

      <RouterLink class="login-back" to="/">← 返回首页</RouterLink>
    </div>
    <p class="login-copy">© 2026 极客栈 GeekStack</p>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px;
  background: var(--bg);
}

.login-card {
  width: min(400px, calc(100vw - 32px));
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px 24px 24px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.login-brand__mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--accent);
  color: var(--accent-text);
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-weight: 700;
}

.login-brand__name {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.05rem;
}

.login-brand__slogan {
  font-size: 0.72rem;
  color: var(--text-3);
}

.login-tabs {
  display: flex;
  border-bottom: 1px solid var(--border);
  margin-bottom: 16px;
}

.login-tabs button {
  flex: 1;
  border: none;
  background: none;
  padding: 12px 0;
  font-size: 0.95rem;
  color: var(--text-2);
  cursor: pointer;
  font-family: inherit;
  position: relative;
}

.login-tabs button[aria-selected='true'] {
  color: var(--text-1);
  font-weight: 600;
}

.login-tabs button[aria-selected='true']::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--accent);
}

.login-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.login-forgot {
  font-size: 0.85rem;
  color: var(--accent) !important;
}

.login-back {
  display: block;
  margin-top: 16px;
  text-align: center;
  font-size: 0.85rem;
  color: var(--text-2);
  text-decoration: none;
}

.login-back:hover {
  color: var(--accent);
}

.login-copy {
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-3);
}
</style>

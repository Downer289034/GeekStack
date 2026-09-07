<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NDivider,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NTabPane,
  NTabs,
  NText,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { register } from '@/api/auth'
import { fetchAuthorizeUrl, submitOAuthCode, type OAuthProvider } from '@/api/oauth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const tab = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInst | null>(null)
const registerFormRef = ref<FormInst | null>(null)
const submitting = ref(false)
const oauthLoading = ref<OAuthProvider | null>(null)

// OAuth 弹窗回调：后端收尾页 postMessage 回 code，这里转交后端完成登录/绑定
function onOAuthLogin(provider: OAuthProvider) {
  oauthLoading.value = provider
  let popup: Window | null = null
  const cleanup = () => {
    window.removeEventListener('message', onMessage)
    oauthLoading.value = null
  }
  const onMessage = (event: MessageEvent) => {
    // 回调收尾页来自本站后端
    if (event.origin !== window.location.origin) {
      return
    }
    let payload: { provider?: string; code?: string }
    try {
      payload = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
    } catch {
      return
    }
    if (payload.provider !== provider.toLowerCase() || !payload.code) {
      return
    }
    window.removeEventListener('message', onMessage)
    submitOAuthCode(provider, payload.code)
      .then(async (pair) => {
        await auth.applyTokens(pair)
        goHome()
      })
      .catch(() => {
        // 错误提示由请求拦截器统一 toast
      })
      .finally(() => {
        oauthLoading.value = null
        popup?.close()
      })
  }
  window.addEventListener('message', onMessage)
  void fetchAuthorizeUrl(provider)
    .then((url) => {
      popup = window.open(url, 'oauth_' + provider, 'width=680,height=720')
      if (!popup) {
        // 弹窗被拦截：降级为本页跳转（回调后用户手动刷新）
        window.location.assign(url)
        return
      }
      // 弹窗被用户直接关闭时复位按钮
      const timer = window.setInterval(() => {
        if (popup?.closed) {
          window.clearInterval(timer)
          cleanup()
        }
      }, 500)
    })
    .catch(() => {
      cleanup()
    })
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
</script>

<template>
  <div class="login-wrap">
    <n-card class="login-card" title="极客栈">
      <n-tabs v-model:value="tab" type="segment" animated>
        <n-tab-pane name="login" tab="登录">
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
            <n-button type="primary" block :loading="submitting" @click="onLoginSubmit">
              登录
            </n-button>
          </n-form>
        </n-tab-pane>
        <n-tab-pane name="register" tab="注册">
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
        </n-tab-pane>
      </n-tabs>

      <n-divider class="oauth-divider">
        <n-text depth="3" style="font-size: 12px">第三方登录（未注册将自动创建账号）</n-text>
      </n-divider>
      <n-space vertical size="small">
        <n-button block :loading="oauthLoading === 'GITHUB'" @click="onOAuthLogin('GITHUB')">
          使用 GitHub 登录
        </n-button>
        <n-button block :loading="oauthLoading === 'GITEE'" @click="onOAuthLogin('GITEE')">
          使用 Gitee 登录
        </n-button>
      </n-space>
    </n-card>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 420px;
}
</style>

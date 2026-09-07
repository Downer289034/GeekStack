<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { NCard, NSpin } from 'naive-ui'
import { onMounted, onUnmounted } from 'vue'
import { submitOAuthCode, type OAuthProvider } from '@/api/oauth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const provider = String(route.params.provider ?? '').toUpperCase() as OAuthProvider
const storeKey = `oauth_pending_${provider.toLowerCase()}`

function goHome() {
  void router.push('/')
}

onMounted(() => {
  // 后端回调页已将 code 写入 localStorage 并 redirect 到本路由
  // 轮询读取后转交后端完成登录
  let resolved = false
  const pollTimer = window.setInterval(() => {
    if (resolved) return
    try {
      const raw = localStorage.getItem(storeKey)
      if (raw) {
        const parsed = JSON.parse(raw) as { provider?: string; code?: string }
        if (parsed.provider === provider.toLowerCase() && parsed.code) {
          resolved = true
          localStorage.removeItem(storeKey)
          submitOAuthCode(provider, parsed.code)
            .then(async (pair) => {
              await auth.applyTokens(pair)
              goHome()
            })
            .catch(() => {
              // 错误提示由请求拦截器统一 toast
            })
        }
      }
    } catch {
      // ignore
    }
  }, 500)

  onUnmounted(() => {
    window.clearInterval(pollTimer)
  })
})
</script>

<template>
  <div class="oauth-callback-wrap">
    <n-card class="oauth-callback-card">
      <n-spin description="正在完成登录，请稍候…" size="large" />
    </n-card>
  </div>
</template>

<style scoped>
.oauth-callback-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.oauth-callback-card {
  width: 320px;
  text-align: center;
}
</style>

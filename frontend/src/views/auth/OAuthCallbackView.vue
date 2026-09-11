<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { onMounted, onUnmounted } from 'vue'
import { submitOAuthCode, type OAuthProvider } from '@/api/oauth'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
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
    <div class="oauth-callback-card">
      <p class="oauth-hint">正在完成登录，请稍候…</p>
      <GsSkeleton variant="detail" />
    </div>
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
  width: min(420px, 92vw);
}

.oauth-hint {
  margin: 0 0 16px;
  text-align: center;
  color: var(--text-2);
  font-size: 0.925rem;
}
</style>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NEmpty, NIcon } from 'naive-ui'
import { AlertCircleOutline } from '@vicons/ionicons5'
import {
  oauthPendingStorageKey,
  oauthStateStorageKey,
  submitOAuthCode,
  takeExpectedOAuthState,
  type OAuthProvider
} from '@/api/oauth'
import GsButton from '@/components/gs/GsButton.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const provider = String(route.params.provider ?? '').toUpperCase() as OAuthProvider
const storeKey = oauthPendingStorageKey(provider)

const errorHint = ref('')
const busy = ref(true)

function clearPending() {
  localStorage.removeItem(storeKey)
  sessionStorage.removeItem(oauthStateStorageKey(provider))
}

function fail(message: string) {
  busy.value = false
  errorHint.value = message
  clearPending()
}

function goHome() {
  void router.push('/')
}

onMounted(() => {
  if (provider !== 'GITHUB' && provider !== 'GITEE') {
    fail('不支持的登录平台，请返回重试')
    return
  }

  let resolved = false
  const pollTimer = window.setInterval(() => {
    if (resolved) return
    try {
      const raw = localStorage.getItem(storeKey)
      if (!raw) return

      const parsed = JSON.parse(raw) as {
        provider?: string
        code?: string
        state?: string
      }
      if (parsed.provider !== provider.toLowerCase() || !parsed.code) {
        return
      }

      resolved = true
      localStorage.removeItem(storeKey)

      const expected = takeExpectedOAuthState(provider)
      const returned = parsed.state?.trim() ?? ''

      // 比对闭环以 sessionStorage 为准；BE 只验 Redis 存在性
      if (!expected || !returned || expected !== returned) {
        fail('登录校验失败（state 不匹配），请重新发起第三方登录')
        return
      }

      submitOAuthCode(provider, parsed.code, returned)
        .then(async (pair) => {
          await auth.applyTokens(pair)
          goHome()
        })
        .catch((err: unknown) => {
          const msg =
            err && typeof err === 'object' && 'response' in err
              ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
              : err instanceof Error
                ? err.message
                : undefined
          fail(msg || '第三方登录失败，请重新发起')
        })
    } catch {
      // ignore parse errors，继续轮询
    }
  }, 500)

  // 超时：pending 迟迟不到
  const timeoutTimer = window.setTimeout(() => {
    if (!resolved && !errorHint.value) {
      resolved = true
      fail('未收到授权结果，请返回重新登录')
    }
  }, 30000)

  onUnmounted(() => {
    window.clearInterval(pollTimer)
    window.clearTimeout(timeoutTimer)
  })
})
</script>

<template>
  <div class="oauth-callback-wrap">
    <div class="oauth-callback-card">
      <template v-if="errorHint">
        <n-empty size="large" :description="errorHint">
          <template #icon>
            <n-icon :component="AlertCircleOutline" :size="48" />
          </template>
          <template #extra>
            <GsButton variant="primary" to="/login">返回登录</GsButton>
          </template>
        </n-empty>
      </template>
      <template v-else-if="busy">
        <p class="oauth-hint">正在完成登录，请稍候…</p>
        <GsSkeleton variant="detail" />
      </template>
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

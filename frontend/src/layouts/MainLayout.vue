<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import {
  NButton,
  NLayout,
  NLayoutContent,
  NLayoutFooter,
  NLayoutHeader,
  NMessageProvider,
  NText
} from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

onMounted(() => {
  void auth.fetchCurrentUser()
})

const navItems = [
  { name: 'home', label: '首页', to: '/', match: (n: unknown) => n === 'home' },
  {
    name: 'share-plaza',
    label: '项目广场',
    to: '/shares',
    match: (n: unknown) => typeof n === 'string' && n.startsWith('share')
  },
  {
    name: 'forum',
    label: '技术论坛',
    to: '/forum',
    match: (n: unknown) => n === 'forum' || n === 'post-detail'
  }
]

function logout() {
  auth.logout()
}
</script>

<template>
  <n-layout class="layout">
    <n-layout-header class="header" bordered>
      <nav class="nav">
        <RouterLink class="brand" to="/">极客栈 GeekStack</RouterLink>
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          class="nav-item"
          :class="{ active: item.match(route.name) }"
          :to="item.to"
        >
          {{ item.label }}
        </RouterLink>
        <span class="spacer" />
        <template v-if="auth.isLoggedIn">
          <RouterLink to="/shares/new">
            <n-button size="small" type="primary">发布项目</n-button>
          </RouterLink>
          <n-text class="user-name">{{ auth.user?.nickname ?? auth.user?.username ?? '' }}</n-text>
          <n-button quaternary @click="logout">退出登录</n-button>
        </template>
        <RouterLink v-else to="/login">
          <n-button quaternary type="primary">登录 / 注册</n-button>
        </RouterLink>
      </nav>
    </n-layout-header>
    <n-layout-content class="content">
      <n-message-provider>
        <RouterView />
      </n-message-provider>
    </n-layout-content>
    <n-layout-footer class="footer" bordered>
      <span>© 2026 极客栈 GeekStack · 代码江湖的落脚点（备案号占位）</span>
    </n-layout-footer>
  </n-layout>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  background-color: var(--color-bg-container);
}

.nav {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary);
  text-decoration: none;
}

.nav-item {
  color: var(--color-text);
  text-decoration: none;
  font-size: 15px;
  padding: 6px 2px;
  border-bottom: 2px solid transparent;
}

.nav-item.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 500;
}

.spacer {
  flex: 1;
}

.user-name {
  font-size: 14px;
}

.content {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 60px - 48px);
}

.footer {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-size: 13px;
  background-color: var(--color-bg-container);
}
</style>

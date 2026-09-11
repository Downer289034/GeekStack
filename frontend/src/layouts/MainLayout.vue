<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { NIcon, NMessageProvider } from 'naive-ui'
import {
  CreateOutline,
  MoonOutline,
  SearchOutline,
  SunnyOutline
} from '@vicons/ionicons5'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const themeStore = useThemeStore()
const { theme } = storeToRefs(themeStore)

onMounted(() => {
  void auth.fetchCurrentUser()
  window.addEventListener('keydown', onGlobalKey)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKey)
})

const navItems: Array<{
  name: string
  label: string
  to: string
  match: (n: unknown) => boolean
}> = [
  { name: 'home', label: '首页', to: '/', match: (n) => n === 'home' },
  {
    name: 'share-plaza',
    label: '项目广场',
    to: '/shares',
    match: (n) => typeof n === 'string' && n.startsWith('share')
  },
  {
    name: 'forum',
    label: '技术论坛',
    to: '/forum',
    match: (n) => n === 'forum' || n === 'post-detail' || n === 'post-publish'
  },
  {
    name: 'search',
    label: '搜索',
    to: '/search',
    match: (n) => n === 'search' || n === 'tags' || n === 'tag-detail'
  }
]

const avatarLetter = computed(() => {
  const name = auth.user?.nickname ?? auth.user?.username ?? ''
  return name ? name.slice(0, 1).toUpperCase() : '栈'
})

const contentWidth = computed(() => {
  const w = route.meta.containerWidth as string | undefined
  if (w === 'md' || w === 'sm') return w
  return 'lg'
})

function onGlobalKey(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    void router.push({ name: 'search' })
  }
}

function onSearchSubmit(e: Event) {
  e.preventDefault()
  const form = e.target as HTMLFormElement
  const q = new FormData(form).get('q')
  void router.push({
    name: 'search',
    query: typeof q === 'string' && q.trim() ? { q: q.trim() } : undefined
  })
}

function logout() {
  auth.logout()
}
</script>

<template>
  <div class="shell">
    <header class="gs-nav" role="banner">
      <div class="gs-container gs-nav__inner">
        <RouterLink class="gs-logo" to="/" aria-label="极客栈 GeekStack 首页">
          <span class="gs-logo__mark" aria-hidden="true">极</span>
          <span class="gs-logo__text">
            极客栈
            <small>GeekStack</small>
          </span>
        </RouterLink>

        <nav class="gs-nav__links" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.name"
            class="gs-nav__link"
            :class="{ active: item.match(route.name) }"
            :to="item.to"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <form class="gs-search" role="search" @submit="onSearchSubmit">
          <n-icon class="gs-search__icon" :component="SearchOutline" :size="14" />
          <input
            name="q"
            type="search"
            placeholder="搜索帖子、项目…"
            aria-label="搜索"
            autocomplete="off"
          />
          <kbd aria-hidden="true">Ctrl K</kbd>
        </form>

        <button
          type="button"
          class="gs-theme-toggle"
          :title="theme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
          :aria-label="theme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
          @click="themeStore.toggleTheme()"
        >
          <n-icon :component="theme === 'dark' ? SunnyOutline : MoonOutline" :size="16" />
        </button>

        <RouterLink
          v-if="auth.isLoggedIn"
          class="gs-btn-primary"
          to="/forum/new"
        >
          <n-icon :component="CreateOutline" :size="16" />
          写文章
        </RouterLink>
        <RouterLink v-else class="gs-btn-primary" to="/login">写文章</RouterLink>

        <template v-if="auth.isLoggedIn">
          <RouterLink
            class="gs-avatar"
            :to="`/users/${auth.user?.username ?? ''}`"
            :aria-label="`个人主页 ${auth.user?.nickname ?? auth.user?.username ?? ''}`"
          >
            {{ avatarLetter }}
          </RouterLink>
          <button type="button" class="gs-text-btn" @click="logout">退出</button>
        </template>
        <RouterLink v-else class="gs-text-btn" to="/login">登录</RouterLink>
      </div>
    </header>

    <main
      class="gs-shell-main"
      :class="{
        'gs-shell-main--md': contentWidth === 'md',
        'gs-shell-main--sm': contentWidth === 'sm'
      }"
    >
      <n-message-provider>
        <RouterView />
      </n-message-provider>
    </main>

    <footer class="gs-footer">
      <div class="gs-container gs-footer__inner">
        <span>© 2026 极客栈 GeekStack · 代码江湖的落脚点</span>
        <div class="gs-footer__links">
          <a href="#about">关于</a>
          <a href="/api/v1">API</a>
          <span>RSS</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  color: var(--text-1);
}

.gs-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg);
  border-bottom: 1px solid var(--border);
  /* 禁玻璃拟态：无 backdrop-filter / blur */
}

.gs-nav__inner {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 64px;
}

.gs-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-1);
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.125rem;
  letter-spacing: 0.01em;
  flex-shrink: 0;
}

.gs-logo__mark {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-logo);
  background: var(--accent);
  color: var(--accent-text);
  display: grid;
  place-items: center;
  font-size: 15px;
  font-weight: 700;
}

.gs-logo__text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.gs-logo__text small {
  font-weight: 400;
  font-size: 0.68rem;
  color: var(--text-3);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.gs-nav__links {
  display: flex;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.gs-nav__link {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 0.9rem;
  color: var(--text-2);
  text-decoration: none;
  position: relative;
  transition:
    color 0.15s var(--ease-out),
    background 0.15s var(--ease-out);
}

.gs-nav__link:hover {
  color: var(--text-1);
  background: var(--surface-2);
}

.gs-nav__link.active {
  color: var(--text-1);
  font-weight: 600;
}

.gs-nav__link.active::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 2px;
  height: 2px;
  border-radius: 1px;
  background: var(--accent);
}

.gs-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 7px 12px;
  width: 220px;
  transition:
    border-color 0.15s var(--ease-out),
    width 0.25s var(--ease-out);
}

.gs-search:focus-within {
  border-color: var(--accent);
  width: 280px;
}

.gs-search__icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: var(--text-3);
}

.gs-search input {
  border: none;
  background: none;
  outline: none;
  color: var(--text-1);
  width: 100%;
  font-size: 0.875rem;
  font-family: inherit;
}

.gs-search input::placeholder {
  color: var(--text-3);
}

.gs-search kbd {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  color: var(--text-3);
  border: 1px solid var(--border-strong);
  border-radius: 4px;
  padding: 1px 5px;
  flex-shrink: 0;
}

.gs-theme-toggle {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface-1);
  color: var(--text-2);
  display: grid;
  place-items: center;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
  transition:
    color 0.15s var(--ease-out),
    border-color 0.15s var(--ease-out);
}

.gs-theme-toggle:hover {
  color: var(--accent);
  border-color: var(--accent);
}

.gs-theme-toggle :deep(svg),
.gs-theme-toggle svg {
  width: 16px;
  height: 16px;
}

.gs-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--accent);
  color: var(--accent-text);
  border: none;
  border-radius: 10px;
  padding: 9px 16px;
  font-weight: 600;
  font-size: 0.9rem;
  text-decoration: none;
  white-space: nowrap;
  transition:
    background 0.15s var(--ease-out),
    transform 0.2s var(--ease-out);
}

.gs-btn-primary:hover {
  background: var(--accent-hover);
  transform: translateY(-1px);
}

.gs-btn-primary__icon {
  width: 16px;
  height: 16px;
}

.gs-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--surface-3);
  border: 2px solid var(--border-strong);
  display: grid;
  place-items: center;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-1);
  text-decoration: none;
  flex-shrink: 0;
}

.gs-text-btn {
  border: none;
  background: none;
  color: var(--text-2);
  font-size: 0.85rem;
  cursor: pointer;
  text-decoration: none;
  padding: 6px 4px;
  font-family: inherit;
}

.gs-text-btn:hover {
  color: var(--accent);
}

.gs-shell-main {
  flex: 1;
  width: 100%;
  max-width: var(--container-max);
  margin: 0 auto;
  padding: 24px 24px 48px;
  min-width: 0;
}

.gs-shell-main--md {
  max-width: 900px;
}

.gs-shell-main--sm {
  max-width: 760px;
}

.gs-footer {
  border-top: 1px solid var(--border);
  padding: 20px 0 28px;
  color: var(--text-3);
  font-size: 0.8rem;
}

.gs-footer__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.gs-footer__links {
  display: flex;
  gap: 16px;
}

.gs-footer__links a {
  color: var(--text-2);
  text-decoration: none;
}

.gs-footer__links a:hover {
  color: var(--accent);
}

@media (max-width: 960px) {
  .gs-nav__links,
  .gs-search {
    display: none;
  }
}

@media (max-width: 560px) {
  .gs-nav__inner {
    height: auto;
    min-height: 56px;
    padding-top: 8px;
    padding-bottom: 8px;
    flex-wrap: wrap;
    gap: 10px;
  }

  .gs-btn-primary {
    padding: 8px 12px;
    font-size: 0.82rem;
  }
}
</style>

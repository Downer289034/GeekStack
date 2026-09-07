<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { NAvatar, NButton, NCard, NEmpty, NTag, NText } from 'naive-ui'
import { fetchProfile, type ProfileAggregate } from '@/api/profile'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

const data = ref<ProfileAggregate | null>(null)
const loading = ref(true)
const failed = ref(false)

const username = computed(() => String(route.params.username ?? ''))
const isOwner = computed(() => auth.isLoggedIn && auth.user?.username === username.value)

async function load() {
  loading.value = true
  failed.value = false
  try {
    data.value = await fetchProfile(username.value)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

function formatTime(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

watch(username, () => {
  void load()
})

onMounted(() => {
  void load()
})
</script>

<template>
  <div v-if="failed" class="profile-404">
    <n-empty size="huge" description="用户不存在">
      <template #extra>
        <RouterLink to="/">
          <n-button>回到首页</n-button>
        </RouterLink>
      </template>
    </n-empty>
  </div>

  <div v-else class="profile">
    <n-card class="profile-card">
      <div class="profile-head">
        <n-avatar round :size="64" class="profile-avatar">{{ data?.profile.nickname?.charAt(0) ?? '?' }}</n-avatar>
        <div class="profile-info">
          <div class="profile-name-row">
            <h2 class="profile-name">{{ data?.profile.nickname ?? data?.profile.username ?? '加载中…' }}</h2>
            <n-tag v-if="data?.profile.role === 'ADMIN'" size="small" type="error" :bordered="false">管理员</n-tag>
            <n-tag v-if="isOwner" size="small" type="info" :bordered="false">这是你</n-tag>
          </div>
          <n-text depth="3" class="profile-sub">@{{ data?.profile.username }} · 加入于 {{ formatTime(data?.profile.createdAt ?? null) }}</n-text>
          <p class="profile-bio">{{ data?.profile.bio || '这个人很懒，什么都没写' }}</p>
        </div>
      </div>
    </n-card>

    <n-card title="项目分享" class="profile-section">
      <n-empty v-if="data && data.shares.length === 0" description="还没有分享项目" size="small" />
      <div v-for="share in data?.shares ?? []" :key="share.id" class="profile-item">
        <RouterLink :to="`/shares/${share.id}`" class="profile-item-link">
          <span class="profile-item-title">{{ share.title }}</span>
          <n-tag v-if="share.status === 'HIDDEN'" size="small" type="warning" :bordered="false">已下架</n-tag>
        </RouterLink>
        <n-text depth="3" class="profile-item-meta">{{ share.summary }}</n-text>
      </div>
    </n-card>

    <n-card title="帖子" class="profile-section">
      <n-empty v-if="data && data.posts.length === 0" description="还没有发过帖子" size="small" />
      <div v-for="post in data?.posts ?? []" :key="post.id" class="profile-item">
        <RouterLink :to="`/forum/posts/${post.id}`" class="profile-item-link">
          <span class="profile-item-title">{{ post.title }}</span>
        </RouterLink>
        <n-text depth="3" class="profile-item-meta">
          回帖 {{ post.replyCount }} · 浏览 {{ post.viewCount }} · {{ formatTime(post.createdAt) }}
        </n-text>
      </div>
    </n-card>
  </div>
</template>

<style scoped>
.profile-card {
  margin-bottom: 16px;
}

.profile-head {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.profile-avatar {
  background: var(--color-primary);
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-name {
  margin: 0;
  font-size: 20px;
}

.profile-sub {
  font-size: 13px;
}

.profile-bio {
  margin: 8px 0 0;
  color: var(--color-text);
}

.profile-section {
  margin-bottom: 16px;
}

.profile-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--color-bg-layout);
}

.profile-item:last-child {
  border-bottom: none;
}

.profile-item-link {
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-item-title {
  color: var(--color-text);
  font-weight: 500;
}

.profile-item-meta {
  display: block;
  margin-top: 4px;
  font-size: 12px;
}

.profile-404 {
  max-width: 480px;
  margin: 96px auto;
}
</style>

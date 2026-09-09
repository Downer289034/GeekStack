<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { NAvatar, NButton, NCard, NEmpty, NIcon, NTag, NText, useMessage } from 'naive-ui'
import { CubeOutline, CreateOutline, PersonOutline } from '@vicons/ionicons5'
import { fetchProfile, type ProfileAggregate } from '@/api/profile'
import { fetchRepos, toggleRepoHide, type UserRepo } from '@/api/repo'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const message = useMessage()

const profile = ref<ProfileAggregate | null>(null)
const repos = ref<UserRepo[]>([])
const loading = ref(true)
const failed = ref(false)

const username = computed(() => String(route.params.username ?? ''))
const isOwner = computed(() => auth.isLoggedIn && auth.user?.username === username.value)

async function loadData() {
  loading.value = true
  failed.value = false
  try {
    profile.value = await fetchProfile(username.value)
    repos.value = await fetchRepos(username.value)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function onToggleHide(repo: UserRepo) {
  try {
    const nowHidden = await toggleRepoHide(repo.id)
    repo.hidden = nowHidden ? 1 : 0
    message.success(nowHidden ? '已隐藏（不会展示在主页）' : '已恢复显示')
    // 隐藏后从可见列表中移除
    if (nowHidden) {
      repos.value = repos.value.filter((r) => r.id !== repo.id)
    }
  } catch {
    message.error('操作失败')
  }
}

function formatTime(iso: string): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

watch(username, () => {
  void loadData()
})

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div v-if="failed" class="profile-404">
    <n-empty size="huge" description="用户不存在">
      <template #icon>
        <n-icon :component="PersonOutline" :size="48" />
      </template>
      <template #extra>
        <RouterLink to="/"><n-button type="primary">回到首页</n-button></RouterLink>
      </template>
    </n-empty>
  </div>

  <div v-else class="profile">
    <n-card class="profile-card gs-card-detail" :bordered="false">
      <div class="profile-head">
        <n-avatar round :size="64" class="profile-avatar">
          {{ profile?.profile.nickname?.charAt(0) ?? '?' }}
        </n-avatar>
        <div class="profile-info">
          <div class="profile-name-row">
            <h2 class="profile-name">{{ profile?.profile.nickname ?? profile?.profile.username ?? '加载中…' }}</h2>
            <n-tag v-if="profile?.profile.role === 'ADMIN'" size="small" type="error" :bordered="false">管理员</n-tag>
            <n-tag v-if="isOwner" size="small" type="info" :bordered="false">这是你</n-tag>
          </div>
          <n-text depth="3" class="profile-sub">
            @{{ profile?.profile.username }} · 加入于 {{ formatTime(profile?.profile.createdAt ?? '') }}
          </n-text>
          <p class="profile-bio">{{ profile?.profile.bio || '这个人很懒，什么都没写' }}</p>
        </div>
      </div>
    </n-card>

    <!-- 仓库卡片区（T17 收尾） -->
    <n-card title="我的仓库" class="profile-section gs-card-detail" :bordered="false">
      <n-empty v-if="!loading && repos.length === 0" description="未绑定仓库或同步失败" size="small" />
      <div v-for="repo in repos" :key="repo.id" class="repo-card">
        <div class="repo-card-head">
          <a :href="repo.repoUrl" target="_blank" rel="noopener noreferrer" class="repo-name">
            {{ repo.repoFullName }}
          </a>
          <n-tag size="small" :bordered="false">⭐ {{ repo.stars }}</n-tag>
        </div>
        <p v-if="repo.description" class="repo-desc">{{ repo.description }}</p>
        <div class="repo-card-foot">
          <n-tag size="small" type="info" :bordered="false">{{ repo.provider }}</n-tag>
          <n-text depth="3" class="repo-synced">
            同步于 {{ formatTime(repo.syncedAt) }}
          </n-text>
          <n-button v-if="isOwner" text size="tiny" @click="onToggleHide(repo)">
            隐藏
          </n-button>
        </div>
      </div>
    </n-card>

    <n-card title="项目分享" class="profile-section gs-card-detail" :bordered="false">
      <n-empty v-if="profile && profile.shares.length === 0" description="还没有分享项目" size="small">
        <template #icon>
          <n-icon :component="CubeOutline" :size="36" />
        </template>
        <template v-if="isOwner" #extra>
          <RouterLink to="/shares/new"><n-button size="small" type="primary">去发布</n-button></RouterLink>
        </template>
      </n-empty>
      <div v-for="share in (profile?.shares ?? [])" :key="share.id" class="profile-item">
        <RouterLink :to="`/shares/${share.id}`" class="profile-item-link">
          <span class="profile-item-title">{{ share.title }}</span>
          <n-tag v-if="share.status === 'HIDDEN'" size="small" type="error" :bordered="false">已下架</n-tag>
        </RouterLink>
        <n-text depth="3" class="profile-item-meta">{{ share.summary }}</n-text>
      </div>
    </n-card>

    <n-card title="帖子" class="profile-section gs-card-detail" :bordered="false">
      <n-empty v-if="profile && profile.posts.length === 0" description="还没有发过帖子" size="small">
        <template #icon>
          <n-icon :component="CreateOutline" :size="36" />
        </template>
        <template v-if="isOwner" #extra>
          <RouterLink to="/forum/new"><n-button size="small" type="primary">去发帖</n-button></RouterLink>
        </template>
      </n-empty>
      <div v-for="post in (profile?.posts ?? [])" :key="post.id" class="profile-item">
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
  flex-wrap: wrap;
}

.profile-avatar {
  background: var(--color-primary);
  color: var(--color-bg-container);
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

/* 仓库卡片 */
.repo-card {
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.repo-card:last-child {
  border-bottom: none;
}

.repo-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.repo-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-primary);
  text-decoration: none;
}

.repo-name:hover {
  text-decoration: underline;
}

.repo-desc {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.repo-card-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}

.repo-synced {
  font-size: 12px;
}

/* 项目/帖子列表 */
.profile-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
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

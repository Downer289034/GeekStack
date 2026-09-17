<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NAvatar, NEmpty, NIcon, useMessage } from 'naive-ui'
import { CubeOutline, CreateOutline, PersonOutline } from '@vicons/ionicons5'
import { fetchProfile, type ProfileAggregate } from '@/api/profile'
import { fetchRepos, toggleRepoHide, type UserRepo } from '@/api/repo'
import GsBadge from '@/components/gs/GsBadge.vue'
import GsButton from '@/components/gs/GsButton.vue'
import GsCodeChip from '@/components/gs/GsCodeChip.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsTag from '@/components/gs/GsTag.vue'
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
        <GsButton variant="primary" to="/">回到首页</GsButton>
      </template>
    </n-empty>
  </div>

  <div v-else class="profile">
    <header class="profile-head">
      <n-avatar round :size="64" class="profile-avatar">
        {{ profile?.profile.nickname?.charAt(0) ?? '?' }}
      </n-avatar>
      <div class="profile-info">
        <div class="profile-name-row">
          <h2 class="profile-name font-display">
            {{ profile?.profile.nickname ?? profile?.profile.username ?? '加载中…' }}
          </h2>
          <GsBadge v-if="profile?.profile.role === 'ADMIN'" tone="error">管理员</GsBadge>
          <GsBadge v-if="isOwner" tone="info">这是你</GsBadge>
        </div>
        <p class="profile-sub font-mono">
          @{{ profile?.profile.username }} · 加入于 {{ formatTime(profile?.profile.createdAt ?? '') }}
        </p>
        <p class="profile-bio">{{ profile?.profile.bio || '这个人很懒，什么都没写' }}</p>
      </div>
    </header>

    <section class="profile-section">
      <h3 class="section-title font-display">我的仓库</h3>
      <n-empty v-if="!loading && repos.length === 0" description="未绑定仓库或同步失败" size="small" />
      <article v-for="repo in repos" :key="repo.id" class="repo-card">
        <div class="repo-card-head">
          <a :href="repo.repoUrl" target="_blank" rel="noopener noreferrer" class="repo-name font-mono">
            {{ repo.repoFullName }}
          </a>
          <GsTag>★ {{ repo.stars }}</GsTag>
        </div>
        <p v-if="repo.description" class="repo-desc">{{ repo.description }}</p>
        <div class="repo-card-foot">
          <GsCodeChip>{{ repo.provider }}</GsCodeChip>
          <span class="repo-synced font-mono">同步于 {{ formatTime(repo.syncedAt) }}</span>
          <GsButton v-if="isOwner" variant="text" @click="onToggleHide(repo)">隐藏</GsButton>
        </div>
      </article>
    </section>

    <section class="profile-section">
      <h3 class="section-title font-display">项目分享</h3>
      <n-empty v-if="profile && profile.shares.length === 0" description="还没有分享项目" size="small">
        <template #icon>
          <n-icon :component="CubeOutline" :size="36" />
        </template>
        <template v-if="isOwner" #extra>
          <GsButton variant="primary" to="/shares/new">去发布</GsButton>
        </template>
      </n-empty>
      <GsFeedRow
        v-for="share in profile?.shares ?? []"
        :key="share.id"
        :title="share.title"
        :to="`/shares/${share.id}`"
        :excerpt="share.summary"
        :tags="share.status === 'HIDDEN' ? ['已下架'] : undefined"
      />
    </section>

    <section class="profile-section">
      <h3 class="section-title font-display">帖子</h3>
      <n-empty v-if="profile && profile.posts.length === 0" description="还没有发过帖子" size="small">
        <template #icon>
          <n-icon :component="CreateOutline" :size="36" />
        </template>
        <template v-if="isOwner" #extra>
          <GsButton variant="primary" to="/forum/new">去发帖</GsButton>
        </template>
      </n-empty>
      <GsFeedRow
        v-for="post in profile?.posts ?? []"
        :key="post.id"
        :title="post.title"
        :to="`/forum/posts/${post.id}`"
        :tags="post.tags ?? []"
        :meta="`回帖 ${post.replyCount} · 浏览 ${post.viewCount} · ${formatTime(post.createdAt)}`"
      />
    </section>
  </div>
</template>

<style scoped>
.profile-head {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.profile-avatar {
  background: var(--accent);
  color: var(--accent-text);
  font-size: 24px;
  flex-shrink: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.profile-name {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--text-1);
}

.profile-sub {
  margin: 6px 0 0;
  font-size: 0.8rem;
  color: var(--text-3);
}

.profile-bio {
  margin: 10px 0 0;
  color: var(--text-2);
  max-width: 62ch;
}

.profile-section {
  margin-bottom: 28px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-1);
}

.repo-card {
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
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
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--accent);
  text-decoration: none;
}

.repo-name:hover {
  text-decoration: underline;
}

.repo-desc {
  margin: 6px 0 0;
  color: var(--text-2);
  font-size: 0.85rem;
}

.repo-card-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.repo-synced {
  font-size: 0.72rem;
  color: var(--text-3);
}

.profile-404 {
  max-width: 480px;
  margin: 96px auto;
}
</style>

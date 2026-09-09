<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  NButton,
  NCard,
  NEmpty,
  NIcon,
  NSpace,
  NSpin,
  NTag,
  NText
} from 'naive-ui'
import {
  AddOutline,
  ChatbubblesOutline,
  CreateOutline,
  CubeOutline,
  FlameOutline,
  GridOutline
} from '@vicons/ionicons5'
import type { Category, PostCard } from '@/api/forum'
import type { ShareCard } from '@/api/share'
import { loadHomeFeed } from '@/utils/homeFeedCache'

const loading = ref(true)
const loadError = ref(false)
const hot = ref<PostCard[]>([])
const shares = ref<ShareCard[]>([])
const posts = ref<PostCard[]>([])
const categories = ref<Category[]>([])

const categoryById = computed(() => {
  const map = new Map<number, Category>()
  for (const cat of categories.value) {
    map.set(cat.id, cat)
  }
  return map
})

function categoryLabel(post: PostCard): string {
  return categoryById.value.get(post.categoryId)?.name ?? '讨论'
}

function formatTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

function coverTone(id: number): string {
  const tones = [0, 8, 16]
  return `color-mix(in srgb, var(--color-share) ${18 + tones[id % 3]}%, var(--color-bg-container))`
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const feed = await loadHomeFeed()
    hot.value = feed.hot
    shares.value = feed.shares
    posts.value = feed.posts
    categories.value = feed.categories
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <section class="home-page" aria-label="首页">
    <div class="hero">
      <div class="hero__deco" aria-hidden="true" />
      <div class="hero__content">
        <p class="hero__brand">极客栈 GeekStack</p>
        <h1 class="hero__slogan">代码江湖的落脚点</h1>
        <p class="hero__sub">极客落脚，技术常新——分享项目、讨论技术，找到同路人</p>
        <div class="hero__actions">
          <RouterLink to="/shares/new">
            <n-button class="hero__btn-primary" type="default" size="medium">
              <template #icon>
                <n-icon :component="AddOutline" />
              </template>
              发布项目
            </n-button>
          </RouterLink>
          <RouterLink to="/forum">
            <n-button class="hero__btn-ghost" ghost size="medium">
              <template #icon>
                <n-icon :component="ChatbubblesOutline" />
              </template>
              逛论坛
            </n-button>
          </RouterLink>
        </div>
      </div>
    </div>

    <n-spin :show="loading">
      <div v-if="loadError" class="home-error">
        <n-empty description="首页动态加载失败，请稍后重试">
          <template #extra>
            <n-button type="primary" @click="load">重试</n-button>
          </template>
        </n-empty>
      </div>

      <div v-else class="home-feed">
        <!-- 热榜 -->
        <section class="feed-block" aria-labelledby="home-hot-title">
          <div class="feed-block__head">
            <h2 id="home-hot-title" class="gs-section-title">
              <n-icon :component="FlameOutline" :size="22" />
              热榜
            </h2>
            <RouterLink class="feed-more" to="/forum">查看更多</RouterLink>
          </div>
          <n-empty v-if="!loading && hot.length === 0" description="暂无热帖" size="small">
            <template #icon>
              <n-icon :component="FlameOutline" :size="40" />
            </template>
            <template #extra>
              <RouterLink to="/forum/new">
                <n-button size="small" type="primary">去发帖</n-button>
              </RouterLink>
            </template>
          </n-empty>
          <div v-else class="hot-list">
            <n-card
              v-for="(post, index) in hot"
              :key="post.id"
              class="gs-card-elevated hot-row"
              :bordered="false"
            >
              <RouterLink :to="`/forum/posts/${post.id}`" class="row-link">
                <div class="row-main">
                  <span v-if="index < 3" class="gs-hot-rank">{{ index + 1 }}</span>
                  <n-tag v-if="post.isPinned === 1" size="small" type="warning" :bordered="false">置顶</n-tag>
                  <n-tag class="gs-tag-forum" size="small" :bordered="false">{{ categoryLabel(post) }}</n-tag>
                  <span class="row-title">{{ post.title }}</span>
                </div>
                <div class="row-meta">
                  <n-text depth="3">回帖 {{ post.replyCount }}</n-text>
                  <n-text depth="3">浏览 {{ post.viewCount }}</n-text>
                </div>
              </RouterLink>
            </n-card>
          </div>
        </section>

        <!-- 最新项目 -->
        <section class="feed-block" aria-labelledby="home-shares-title">
          <div class="feed-block__head">
            <h2 id="home-shares-title" class="gs-section-title">
              <n-icon :component="GridOutline" :size="22" />
              最新项目
            </h2>
            <RouterLink class="feed-more" to="/shares">查看更多</RouterLink>
          </div>
          <n-empty v-if="!loading && shares.length === 0" description="暂无项目分享" size="small">
            <template #icon>
              <n-icon :component="CubeOutline" :size="40" />
            </template>
            <template #extra>
              <RouterLink to="/shares/new">
                <n-button size="small" type="primary">去发布</n-button>
              </RouterLink>
            </template>
          </n-empty>
          <div v-else class="share-grid">
            <RouterLink
              v-for="card in shares"
              :key="card.id"
              :to="`/shares/${card.id}`"
              class="card-link"
            >
              <n-card class="gs-card-elevated plaza-card" :bordered="false">
                <div class="card-cover" :style="{ background: coverTone(card.id) }" aria-hidden="true">
                  <span class="card-cover__grid" />
                  <n-tag class="gs-tag-share card-cover__badge" size="small" :bordered="false">项目</n-tag>
                </div>
                <div class="card-body">
                  <div class="card-title-row">
                    <span class="card-title">{{ card.title }}</span>
                    <n-tag size="small" :bordered="false">浏览 {{ card.viewCount }}</n-tag>
                  </div>
                  <p class="card-summary">{{ card.summary }}</p>
                  <div class="card-footer">
                    <n-space size="small">
                      <n-tag
                        v-for="t in card.tags ?? []"
                        :key="t"
                        class="gs-tag-share"
                        size="small"
                        :bordered="false"
                      >
                        {{ t }}
                      </n-tag>
                    </n-space>
                    <n-text depth="3" class="card-time">{{ formatTime(card.createdAt) }}</n-text>
                  </div>
                </div>
              </n-card>
            </RouterLink>
          </div>
        </section>

        <!-- 最新帖子 -->
        <section class="feed-block" aria-labelledby="home-posts-title">
          <div class="feed-block__head">
            <h2 id="home-posts-title" class="gs-section-title">
              <n-icon :component="ChatbubblesOutline" :size="22" />
              最新帖子
            </h2>
            <RouterLink class="feed-more" to="/forum">查看更多</RouterLink>
          </div>
          <n-empty v-if="!loading && posts.length === 0" description="暂无帖子" size="small">
            <template #icon>
              <n-icon :component="CreateOutline" :size="40" />
            </template>
            <template #extra>
              <RouterLink to="/forum/new">
                <n-button size="small" type="primary">去发帖</n-button>
              </RouterLink>
            </template>
          </n-empty>
          <div v-else class="post-list">
            <n-card
              v-for="post in posts"
              :key="post.id"
              class="gs-card-elevated post-row"
              :bordered="false"
            >
              <RouterLink :to="`/forum/posts/${post.id}`" class="row-link">
                <div class="row-main">
                  <n-tag v-if="post.isPinned === 1" size="small" type="warning" :bordered="false">置顶</n-tag>
                  <n-tag class="gs-tag-forum" size="small" :bordered="false">{{ categoryLabel(post) }}</n-tag>
                  <span class="row-title">{{ post.title }}</span>
                </div>
                <div class="row-meta">
                  <n-text depth="3">回帖 {{ post.replyCount }}</n-text>
                  <n-text depth="3">浏览 {{ post.viewCount }}</n-text>
                  <n-text depth="3">{{ formatTime(post.createdAt) }}</n-text>
                </div>
              </RouterLink>
            </n-card>
          </div>
        </section>
      </div>
    </n-spin>
  </section>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero {
  position: relative;
  overflow: hidden;
  min-height: 180px;
  padding: 28px 32px;
  border-radius: var(--radius);
  background: linear-gradient(
    135deg,
    var(--color-primary-pressed) 0%,
    var(--color-primary) 45%,
    var(--color-primary-soft) 100%
  );
  box-shadow: var(--shadow-sm);
  color: var(--color-on-primary);
}

.hero__deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(
      circle at 88% 28%,
      color-mix(in srgb, var(--color-on-primary) 15%, transparent) 0%,
      transparent 42%
    ),
    radial-gradient(
      circle at 72% 78%,
      color-mix(in srgb, var(--color-on-primary) 10%, transparent) 0%,
      transparent 36%
    ),
    repeating-linear-gradient(
      -12deg,
      transparent 0,
      transparent 14px,
      color-mix(in srgb, var(--color-on-primary) 6%, transparent) 14px,
      color-mix(in srgb, var(--color-on-primary) 6%, transparent) 15px
    );
  opacity: 0.9;
}

.hero__content {
  position: relative;
  z-index: 1;
  max-width: 640px;
}

.hero__brand {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.04em;
  opacity: 0.92;
}

.hero__slogan {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--color-on-primary);
}

.hero__sub {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.55;
  opacity: 0.92;
  max-width: 36em;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero__btn-primary {
  --n-color: var(--color-on-primary) !important;
  --n-color-hover: color-mix(in srgb, var(--color-primary) 6%, var(--color-on-primary)) !important;
  --n-color-pressed: color-mix(in srgb, var(--color-primary) 12%, var(--color-on-primary)) !important;
  --n-color-focus: var(--color-on-primary) !important;
  --n-text-color: var(--color-primary) !important;
  --n-text-color-hover: var(--color-primary) !important;
  --n-text-color-pressed: var(--color-primary-pressed) !important;
  --n-border: 1px solid var(--color-on-primary) !important;
  --n-border-hover: 1px solid var(--color-on-primary) !important;
  --n-border-pressed: 1px solid var(--color-on-primary) !important;
  --n-border-focus: 1px solid var(--color-on-primary) !important;
  font-weight: 600;
}

.hero__btn-ghost {
  --n-text-color: var(--color-on-primary) !important;
  --n-text-color-hover: var(--color-on-primary) !important;
  --n-text-color-pressed: var(--color-on-primary) !important;
  --n-border: 1px solid color-mix(in srgb, var(--color-on-primary) 85%, transparent) !important;
  --n-border-hover: 1px solid var(--color-on-primary) !important;
  --n-border-pressed: 1px solid var(--color-on-primary) !important;
  --n-border-focus: 1px solid var(--color-on-primary) !important;
  --n-color: transparent !important;
  --n-color-hover: color-mix(in srgb, var(--color-on-primary) 12%, transparent) !important;
  --n-color-pressed: color-mix(in srgb, var(--color-on-primary) 18%, transparent) !important;
  --n-color-focus: transparent !important;
  font-weight: 500;
}

.home-feed {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.home-error {
  padding: 48px 0;
}

.feed-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.feed-more {
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: none;
  white-space: nowrap;
}

.feed-more:hover {
  text-decoration: underline;
}

.hot-list,
.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-row,
.post-row {
  margin: 0;
}

.row-link {
  text-decoration: none;
  display: block;
  min-width: 0;
}

.row-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.row-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  min-width: 0;
}

.row-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 6px;
  font-size: 12px;
}

.share-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .share-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .share-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    padding: 24px 20px;
    min-height: 168px;
  }

  .hero__slogan {
    font-size: 24px;
  }
}

@media (max-width: 480px) {
  .hero {
    padding: 20px 16px;
    min-height: 160px;
  }

  .hero__slogan {
    font-size: 22px;
  }

  .hero__sub {
    font-size: 13px;
    margin-bottom: 16px;
  }

  .hero__actions {
    width: 100%;
  }

  .hero__actions :deep(a) {
    flex: 1 1 auto;
  }

  .hero__actions :deep(.n-button) {
    width: 100%;
  }
}

.card-link {
  text-decoration: none;
  display: block;
  min-width: 0;
}

.plaza-card {
  overflow: hidden;
  height: 100%;
}

.plaza-card :deep(.n-card__content) {
  padding: 0;
}

.card-cover {
  position: relative;
  height: 96px;
  overflow: hidden;
}

.card-cover__grid {
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      -18deg,
      transparent 0,
      transparent 10px,
      color-mix(in srgb, var(--color-share) 10%, transparent) 10px,
      color-mix(in srgb, var(--color-share) 10%, transparent) 11px
    ),
    radial-gradient(
      circle at 86% 20%,
      color-mix(in srgb, var(--color-share-text) 18%, transparent),
      transparent 45%
    );
  opacity: 0.9;
}

.card-cover__badge {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 1;
}

.card-body {
  padding: 12px 14px 14px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-summary {
  margin: 8px 0 10px;
  color: var(--color-text-secondary);
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.card-time {
  font-size: 12px;
  white-space: nowrap;
}
</style>

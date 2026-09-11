<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { NButton, NEmpty, NSpin, NTag } from 'naive-ui'
import type { Category, PostCard } from '@/api/forum'
import type { ShareCard } from '@/api/share'
import { loadHomeFeed } from '@/utils/homeFeedCache'

type FeedTab = 'latest' | 'hot' | 'qa' | 'share'

const loading = ref(true)
const loadError = ref(false)
const hot = ref<PostCard[]>([])
const shares = ref<ShareCard[]>([])
const posts = ref<PostCard[]>([])
const categories = ref<Category[]>([])
const feedTab = ref<FeedTab>('latest')

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

/** 主栏帖子流：最新=posts，热门=hot，其余按分类名粗过滤 */
const feedPosts = computed(() => {
  if (feedTab.value === 'hot') return hot.value.slice(0, 8)
  if (feedTab.value === 'latest') return posts.value.slice(0, 8)
  if (feedTab.value === 'qa') {
    return posts.value.filter((p) => /问|答/.test(categoryLabel(p))).slice(0, 8)
  }
  return posts.value.filter((p) => /分享|资源/.test(categoryLabel(p))).slice(0, 8)
})

const topicChips = computed(() => {
  const counts = new Map<string, number>()
  for (const s of shares.value) {
    for (const t of s.tags ?? []) {
      counts.set(t, (counts.get(t) ?? 0) + 1)
    }
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
    .map(([name]) => name)
})

const miniShares = computed(() => shares.value.slice(0, 3))

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
  <div class="home">
    <!-- ① Hero：≤120px，墨蓝实色，禁渐变 -->
    <header class="home-hero">
      <div class="home-hero__row">
        <div class="home-hero__copy">
          <h1 class="home-hero__title">
            代码为灯，技术为栈——欢迎<em>夜宿极客栈</em>
          </h1>
          <p class="home-hero__lead">分享项目、讨论技术，找到同路人</p>
        </div>
        <div class="home-hero__actions">
          <RouterLink class="home-hero__btn home-hero__btn--primary" to="/shares/new">发布项目</RouterLink>
          <RouterLink class="home-hero__btn home-hero__btn--ghost" to="/forum">逛论坛</RouterLink>
        </div>
      </div>
    </header>

    <n-spin :show="loading">
      <div v-if="loadError" class="home-error">
        <n-empty description="首页动态加载失败，请稍后重试">
          <template #extra>
            <n-button type="primary" @click="load">重试</n-button>
          </template>
        </n-empty>
      </div>

      <div v-else class="gs-main-grid home-grid">
        <!-- ② 主栏：Tab + 帖子流 -->
        <section class="home-feed" aria-label="客栈动态">
          <div class="feed-head">
            <h2 class="feed-head__title">客栈动态</h2>
            <div class="feed-tabs" role="tablist" aria-label="动态过滤">
              <button
                type="button"
                role="tab"
                :aria-selected="feedTab === 'latest'"
                @click="feedTab = 'latest'"
              >
                最新
              </button>
              <button
                type="button"
                role="tab"
                :aria-selected="feedTab === 'hot'"
                @click="feedTab = 'hot'"
              >
                热门
              </button>
              <button
                type="button"
                role="tab"
                :aria-selected="feedTab === 'qa'"
                @click="feedTab = 'qa'"
              >
                问答
              </button>
              <button
                type="button"
                role="tab"
                :aria-selected="feedTab === 'share'"
                @click="feedTab = 'share'"
              >
                资源分享
              </button>
            </div>
            <RouterLink class="feed-more" to="/forum">更多</RouterLink>
          </div>

          <n-empty
            v-if="!loading && feedPosts.length === 0"
            description="这个分类暂时还没有新内容，去写第一篇吧。"
          >
            <template #extra>
              <RouterLink to="/forum/new">
                <n-button type="primary">去发帖</n-button>
              </RouterLink>
            </template>
          </n-empty>

          <ul v-else class="feed-list">
            <li v-for="post in feedPosts" :key="post.id" class="feed-row">
              <RouterLink :to="`/forum/posts/${post.id}`" class="feed-row__link">
                <div class="feed-row__main">
                  <n-tag v-if="post.isPinned === 1" size="small" type="warning" :bordered="false">
                    置顶
                  </n-tag>
                  <span class="feed-row__cat">{{ categoryLabel(post) }}</span>
                  <span class="feed-row__title">{{ post.title }}</span>
                </div>
                <div class="feed-row__meta font-mono">
                  <span>回帖 {{ post.replyCount }}</span>
                  <span>浏览 {{ post.viewCount }}</span>
                  <span>{{ formatTime(post.createdAt) }}</span>
                </div>
              </RouterLink>
            </li>
          </ul>

          <div class="feed-foot">
            <RouterLink class="feed-more" to="/forum">进入论坛 →</RouterLink>
          </div>
        </section>

        <!-- ③ 右栏：热榜 / 最新项目 / 话题 / 数据 -->
        <aside class="gs-sidebar home-side" aria-label="侧栏">
          <section class="side-block">
            <h3 class="side-block__title">热榜 Top5</h3>
            <ol class="hot-list">
              <li v-for="(post, i) in hot.slice(0, 5)" :key="post.id">
                <RouterLink :to="`/forum/posts/${post.id}`" class="hot-item">
                  <span class="hot-item__rank" :class="{ 'is-top': i < 3 }">{{ i + 1 }}</span>
                  <span class="hot-item__title">{{ post.title }}</span>
                </RouterLink>
              </li>
            </ol>
          </section>

          <section class="side-block">
            <div class="side-block__head">
              <h3 class="side-block__title">最新项目</h3>
              <RouterLink class="feed-more" to="/shares">全部</RouterLink>
            </div>
            <ul class="mini-shares">
              <li v-for="card in miniShares" :key="card.id">
                <RouterLink :to="`/shares/${card.id}`" class="mini-share">
                  <span class="mini-share__glyph" aria-hidden="true">{{ card.title.slice(0, 1) }}</span>
                  <span class="mini-share__body">
                    <span class="mini-share__title">{{ card.title }}</span>
                    <span class="mini-share__tags">
                      <span v-for="t in (card.tags ?? []).slice(0, 2)" :key="t">{{ t }}</span>
                    </span>
                  </span>
                </RouterLink>
              </li>
            </ul>
          </section>

          <section v-if="topicChips.length" class="side-block">
            <h3 class="side-block__title">热门话题</h3>
            <div class="topic-chips">
              <RouterLink
                v-for="name in topicChips"
                :key="name"
                class="topic-chip"
                :to="`/tags/${encodeURIComponent(name)}`"
              >
                {{ name }}
              </RouterLink>
            </div>
          </section>

          <p class="side-stats font-mono">
            热帖 {{ hot.length }} · 项目 {{ shares.length }} · 帖子 {{ posts.length }}
          </p>
        </aside>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.home-hero {
  max-height: 120px;
  margin: 0 0 32px;
  padding: 20px 0;
  border-bottom: 1px solid var(--border);
  background: var(--surface-1);
  border-radius: var(--radius);
  padding-left: 20px;
  padding-right: 20px;
}

.home-hero__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  min-height: 72px;
}

.home-hero__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.25rem;
  line-height: 1.35;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-1);
}

.home-hero__title em {
  font-style: normal;
  color: var(--accent);
}

.home-hero__lead {
  margin: 6px 0 0;
  font-size: 0.875rem;
  color: var(--text-2);
}

.home-hero__actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.home-hero__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  transition:
    background 0.15s var(--ease-out),
    border-color 0.15s var(--ease-out),
    color 0.15s var(--ease-out),
    transform 0.2s var(--ease-out);
}

.home-hero__btn--primary {
  background: var(--accent);
  color: var(--accent-text);
}

.home-hero__btn--primary:hover {
  background: var(--accent-hover);
  transform: translateY(-1px);
}

.home-hero__btn--ghost {
  background: transparent;
  color: var(--text-1);
  border: 1px solid var(--border-strong);
}

.home-hero__btn--ghost:hover {
  color: var(--accent);
  border-color: var(--accent);
}

.home-grid {
  padding-bottom: 24px;
}

.home-error {
  padding: 48px 0;
}

.feed-head {
  display: flex;
  align-items: baseline;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}

.feed-head__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.333rem;
  letter-spacing: -0.01em;
}

.feed-tabs {
  display: flex;
  gap: 16px;
  flex: 1;
}

.feed-tabs button {
  border: none;
  background: none;
  color: var(--text-2);
  padding: 6px 2px;
  font-size: 0.85rem;
  font-family: inherit;
  cursor: pointer;
  position: relative;
}

.feed-tabs button:hover {
  color: var(--text-1);
}

.feed-tabs button[aria-selected='true'] {
  color: var(--text-1);
  font-weight: 600;
}

.feed-tabs button[aria-selected='true']::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 1px;
  background: var(--accent);
}

.feed-more {
  font-size: 0.85rem;
  color: var(--text-2);
  text-decoration: none;
}

.feed-more:hover {
  color: var(--accent);
}

.feed-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.feed-row {
  border-bottom: 1px solid var(--border);
}

.feed-row__link {
  display: block;
  padding: 16px 0;
  text-decoration: none;
  color: inherit;
  transition: padding-left 0.2s var(--ease-out);
}

.feed-row__link:hover {
  padding-left: 8px;
}

.feed-row__link:hover .feed-row__title {
  color: var(--accent);
}

.feed-row__main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.feed-row__cat {
  font-size: 0.75rem;
  color: var(--text-3);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 1px 8px;
}

.feed-row__title {
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-1);
  transition: color 0.15s var(--ease-out);
}

.feed-row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
  font-size: 0.75rem;
  color: var(--text-3);
}

.feed-foot {
  margin-top: 16px;
}

.side-block {
  margin-bottom: 28px;
}

.side-block__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.side-block__title {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
}

.hot-list,
.mini-shares {
  list-style: none;
  margin: 0;
  padding: 0;
}

.hot-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
  text-decoration: none;
  color: var(--text-1);
}

.hot-item:hover .hot-item__title {
  color: var(--accent);
}

.hot-item__rank {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--text-3);
  min-width: 1.2em;
}

.hot-item__rank.is-top {
  color: var(--text-3);
}

.hot-item__title {
  font-size: 0.875rem;
  line-height: 1.4;
  transition: color 0.15s var(--ease-out);
}

.mini-share {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  text-decoration: none;
  color: inherit;
}

.mini-share__glyph {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-weight: 700;
  color: var(--text-2);
  flex-shrink: 0;
}

.mini-share__title {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}

.mini-share__tags {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.mini-share__tags span {
  font-size: 0.72rem;
  color: var(--text-3);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 0 6px;
}

.topic-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.topic-chip {
  font-size: 0.78rem;
  color: var(--text-2);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 4px 10px;
  text-decoration: none;
}

.topic-chip:hover {
  color: var(--accent);
  border-color: var(--accent);
}

.side-stats {
  margin: 0;
  font-size: 0.72rem;
  color: var(--text-3);
}

@media (max-width: 560px) {
  .home-hero {
    max-height: none;
    padding: 16px;
  }

  .home-hero__title {
    font-size: 1.1rem;
  }

  .feed-tabs {
    width: 100%;
    gap: 12px;
    overflow-x: auto;
  }
}
</style>

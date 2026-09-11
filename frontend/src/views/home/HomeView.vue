<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { NButton, NEmpty, NSpin } from 'naive-ui'
import type { Category, PostCard } from '@/api/forum'
import type { ShareCard } from '@/api/share'
import { loadHomeFeed } from '@/utils/homeFeedCache'
import GsButton from '@/components/gs/GsButton.vue'
import GsFeedFeatured from '@/components/gs/GsFeedFeatured.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsTag from '@/components/gs/GsTag.vue'
import GsUnderlineTabs from '@/components/gs/GsUnderlineTabs.vue'
import type { GsTabItem } from '@/components/gs/GsUnderlineTabs.vue'

type FeedTab = 'latest' | 'hot' | 'qa' | 'share'

const loading = ref(true)
const loadError = ref(false)
const hot = ref<PostCard[]>([])
const shares = ref<ShareCard[]>([])
const posts = ref<PostCard[]>([])
const categories = ref<Category[]>([])
const feedTab = ref<string>('latest')

const tabItems: GsTabItem[] = [
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '热门' },
  { key: 'qa', label: '问答' },
  { key: 'share', label: '资源分享' }
]

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

function postMeta(post: PostCard): string {
  return `回帖 ${post.replyCount} · 浏览 ${post.viewCount} · ${formatTime(post.createdAt)}`
}

/** 主栏列表：最新/热门/分类过滤；第一条可作为头条 */
const feedPosts = computed(() => {
  const tab = feedTab.value as FeedTab
  if (tab === 'hot') return hot.value.slice(0, 8)
  if (tab === 'latest') return posts.value.slice(0, 8)
  if (tab === 'qa') {
    return posts.value.filter((p) => /问|答/.test(categoryLabel(p))).slice(0, 8)
  }
  return posts.value.filter((p) => /分享|资源/.test(categoryLabel(p))).slice(0, 8)
})

const featured = computed(() => feedPosts.value[0] ?? null)
const listPosts = computed(() => feedPosts.value.slice(1))

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
    <header class="home-hero">
      <div class="home-hero__row">
        <div class="home-hero__copy">
          <h1 class="home-hero__title">
            代码为灯，技术为栈——欢迎<em>夜宿极客栈</em>
          </h1>
          <p class="home-hero__lead">分享项目、讨论技术，找到同路人</p>
        </div>
        <div class="home-hero__actions">
          <GsButton variant="ghost" to="/shares/new">发布项目</GsButton>
          <GsButton variant="text" to="/forum">逛论坛</GsButton>
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
        <section class="home-feed" aria-label="客栈动态">
          <div class="feed-head">
            <h2 class="feed-head__title">客栈动态</h2>
            <GsUnderlineTabs v-model="feedTab" :items="tabItems" aria-label="动态过滤" />
            <GsButton variant="text" to="/forum">更多</GsButton>
          </div>

          <n-empty
            v-if="!loading && feedPosts.length === 0"
            description="这个分类暂时还没有新内容，去写第一篇吧。"
          >
            <template #extra>
              <GsButton variant="primary" to="/forum/new">去写第一篇</GsButton>
            </template>
          </n-empty>

          <template v-else>
            <GsFeedFeatured
              v-if="featured"
              :title="featured.title"
              :to="`/forum/posts/${featured.id}`"
              :tags="[categoryLabel(featured)]"
              :pinned="featured.isPinned === 1"
              :meta="postMeta(featured)"
            />
            <GsFeedRow
              v-for="(post, i) in listPosts"
              :key="post.id"
              :rank="i + 2"
              show-rank
              :title="post.title"
              :to="`/forum/posts/${post.id}`"
              :tags="[categoryLabel(post)]"
              :pinned="post.isPinned === 1"
              :meta="postMeta(post)"
            />
          </template>

          <div class="feed-foot">
            <GsButton variant="text" to="/forum">进入论坛 →</GsButton>
          </div>
        </section>

        <aside class="gs-sidebar home-side" aria-label="侧栏">
          <section class="side-block">
            <h3 class="side-block__title">热榜 Top5</h3>
            <GsFeedRow
              v-for="(post, i) in hot.slice(0, 5)"
              :key="post.id"
              :rank="i + 1"
              show-rank
              :title="post.title"
              :to="`/forum/posts/${post.id}`"
            />
          </section>

          <section class="side-block">
            <div class="side-block__head">
              <h3 class="side-block__title">最新项目</h3>
              <GsButton variant="text" to="/shares">全部</GsButton>
            </div>
            <ul class="mini-shares">
              <li v-for="card in miniShares" :key="card.id">
                <RouterLink :to="`/shares/${card.id}`" class="mini-share">
                  <span class="mini-share__glyph" aria-hidden="true">{{ card.title.slice(0, 1) }}</span>
                  <span class="mini-share__body">
                    <span class="mini-share__title">{{ card.title }}</span>
                    <span class="mini-share__tags">
                      <GsTag v-for="t in (card.tags ?? []).slice(0, 2)" :key="t">{{ t }}</GsTag>
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
                :to="`/tags/${encodeURIComponent(name)}`"
              >
                <GsTag>{{ name }}</GsTag>
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
}

.home-hero {
  margin: 0 0 32px;
  padding: 20px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-1);
  border-radius: var(--radius);
}

.home-hero__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  min-height: 72px;
  max-height: 120px;
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

.home-grid {
  padding-bottom: 24px;
}

.home-error {
  padding: 48px 0;
}

.feed-head {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--border);
}

.feed-head__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.333rem;
  letter-spacing: -0.01em;
}

.feed-foot {
  margin-top: 12px;
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
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
}

.mini-shares {
  list-style: none;
  margin: 0;
  padding: 0;
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

.topic-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.topic-chips a {
  text-decoration: none;
}

.side-stats {
  margin: 0;
  font-size: 0.72rem;
  color: var(--text-3);
}

@media (max-width: 560px) {
  .home-hero {
    padding: 16px;
  }

  .home-hero__row {
    max-height: none;
  }

  .home-hero__title {
    font-size: 1.1rem;
  }
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { NButton, NEmpty } from 'naive-ui'
import type { Category, PostCard } from '@/api/forum'
import type { ShareCard } from '@/api/share'
import { loadHomeFeed } from '@/utils/homeFeedCache'
import GsButton from '@/components/gs/GsButton.vue'
import GsFeedFeatured from '@/components/gs/GsFeedFeatured.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsTag from '@/components/gs/GsTag.vue'
import GsUnderlineTabs from '@/components/gs/GsUnderlineTabs.vue'
import HeroTyping, { type HeroTypingSegment } from '@/components/gs/HeroTyping.vue'
import type { GsTabItem } from '@/components/gs/GsUnderlineTabs.vue'

type FeedTab = 'latest' | 'hot' | 'ai' | 'arch'

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
  { key: 'ai', label: 'AI' },
  { key: 'arch', label: '架构' }
]

const heroSegments: HeroTypingSegment[] = [
  { text: '代码为灯，', br: true },
  { text: '技术为栈——欢迎' },
  { text: '夜宿极客栈', em: true },
  { text: '。' }
]

/** 列表卡片无昵称字段时的展示名 */
const AUTHOR_POOL = ['阿栈', '灯芯', '掌柜', '夜行侠', '小二', '栈友']

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

function categorySlug(post: PostCard): string {
  return categoryById.value.get(post.categoryId)?.slug ?? ''
}

function authorName(post: PostCard): string {
  return AUTHOR_POOL[Math.abs(post.userId) % AUTHOR_POOL.length]
}

function authorInitial(post: PostCard): string {
  return authorName(post).slice(0, 1)
}

function formatCompact(n: number): string {
  if (n >= 1000) {
    const v = n / 1000
    return `${v >= 10 ? Math.round(v) : v.toFixed(1).replace(/\.0$/, '')}k`
  }
  return String(n)
}

function formatRelative(iso: string | null | undefined): string {
  if (!iso) return '—'
  const t = new Date(iso).getTime()
  if (Number.isNaN(t)) return '—'
  const diff = Date.now() - t
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr} 小时前`
  const day = Math.floor(hr / 24)
  if (day === 1) return '昨天'
  if (day === 2) return '前天'
  if (day < 7) return `${day} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

function featuredTags(post: PostCard): string[] {
  const tags = [categoryLabel(post)]
  if (post.isPinned === 1) tags.push('置顶')
  else if (post.isHot === 1) tags.push('热门')
  else tags.push('深度')
  return tags.slice(0, 2)
}

function featuredMeta(post: PostCard): string {
  return `${formatRelative(post.createdAt)} · ${formatCompact(post.viewCount)} 阅读 · ${post.replyCount} 回帖`
}

function rowMeta(post: PostCard): string {
  return `${formatRelative(post.createdAt)} · ${formatCompact(post.viewCount)} 阅读`
}

function matchTab(post: PostCard, tab: FeedTab): boolean {
  const label = categoryLabel(post)
  const slug = categorySlug(post)
  if (tab === 'ai') return /ai|人工智能|大模型|llm|rag/i.test(`${label} ${slug}`)
  if (tab === 'arch') return /架构|体系|治理|设计/i.test(`${label} ${slug}`)
  return true
}

/** 主栏列表：最新/热门/分类过滤；第一条可作为头条 */
const feedPosts = computed(() => {
  const tab = feedTab.value as FeedTab
  if (tab === 'hot') return hot.value.slice(0, 8)
  const pool = posts.value
  if (tab === 'latest') return pool.slice(0, 8)
  return pool.filter((p) => matchTab(p, tab)).slice(0, 8)
})

const featured = computed(() => feedPosts.value[0] ?? null)
const listPosts = computed(() => feedPosts.value.slice(1))

const hotRail = computed(() => hot.value.slice(0, 4))

const topicChips = computed(() => {
  const counts = new Map<string, number>()
  for (const s of shares.value) {
    for (const t of s.tags ?? []) {
      counts.set(t, (counts.get(t) ?? 0) + 1)
    }
  }
  const fromShares = [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .map(([name]) => name)
  const fallback = [
    'Spring Boot',
    'LangGraph',
    'Milvus',
    'Vue3',
    'MyBatis-Plus',
    'JDK17',
    'Celery',
    'Prompt'
  ]
  return [...new Set([...fromShares, ...fallback])].slice(0, 8)
})

interface Keeper {
  key: number
  name: string
  initial: string
  subtitle: string
  following: boolean
}

/** 从帖子作者聚合「活跃掌柜」展示（卡片无昵称，用稳定展示名） */
const keepers = computed((): Keeper[] => {
  const counts = new Map<number, { post: PostCard; n: number }>()
  for (const p of [...hot.value, ...posts.value]) {
    const cur = counts.get(p.userId)
    if (cur) cur.n += 1
    else counts.set(p.userId, { post: p, n: 1 })
  }
  return [...counts.entries()]
    .sort((a, b) => b[1].n - a[1].n)
    .slice(0, 3)
    .map(([userId, { post, n }], i) => ({
      key: userId,
      name: authorName(post),
      initial: authorInitial(post),
      subtitle: `${categoryLabel(post)} · ${n} 篇`,
      following: i === 0
    }))
})

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
      <p class="home-hero__kicker font-mono">Java × AI 开发者社区</p>
      <HeroTyping
        :segments="heroSegments"
        aria-label="代码为灯，技术为栈——欢迎夜宿极客栈。"
      />
      <p class="home-hero__lead">
        这里是 Java 与 AI 开发者的落脚点：沉淀实战文章、切磋架构取舍、追踪大模型落地的一手经验。
      </p>
      <div class="home-hero__meta">
        <p class="home-hero__facts font-mono">
          12,847 位开发者 / 3,291 篇原创 / 问答采纳率 98.2% / 平均响应 24h
        </p>
        <div class="home-hero__actions">
          <GsButton variant="ghost" to="/shares/new">发布项目</GsButton>
          <GsButton variant="text" to="/forum">逛论坛</GsButton>
        </div>
      </div>
    </header>

    <GsSkeleton v-if="loading" variant="home" />

    <div v-else-if="loadError" class="home-error">
      <n-empty description="首页动态加载失败，请稍后重试">
        <template #extra>
          <n-button type="primary" @click="load">重试</n-button>
        </template>
      </n-empty>
    </div>

    <div v-else class="gs-main-grid home-grid">
      <section v-reveal class="home-feed" aria-label="客栈动态">
        <div class="feed-head">
          <h2 class="feed-head__title">客栈动态</h2>
          <GsUnderlineTabs v-model="feedTab" :items="tabItems" aria-label="动态过滤" />
        </div>

        <n-empty
          v-if="feedPosts.length === 0"
          description="这个分类暂时还没有新内容，去写第一篇吧。"
        >
          <template #extra>
            <GsButton variant="primary" to="/forum/new">去写第一篇</GsButton>
          </template>
        </n-empty>

        <template v-else>
          <GsFeedFeatured
            v-if="featured"
            v-reveal
            :title="featured.title"
            :to="`/forum/posts/${featured.id}`"
            :tags="featuredTags(featured)"
            :author="authorName(featured)"
            :author-initial="authorInitial(featured)"
            :meta="featuredMeta(featured)"
          />
          <GsFeedRow
            v-for="(post, i) in listPosts"
            :key="post.id"
            v-reveal
            :rank="i + 2"
            show-rank
            :title="post.title"
            :to="`/forum/posts/${post.id}`"
            :author="authorName(post)"
            :meta="rowMeta(post)"
          />
        </template>
      </section>

      <aside v-reveal class="gs-sidebar home-side" aria-label="侧栏">
        <section class="rail-section">
          <h3 class="rail-section__title font-mono">客栈热榜</h3>
          <RouterLink
            v-for="(post, i) in hotRail"
            :key="post.id"
            class="hot-item"
            :to="`/forum/posts/${post.id}`"
          >
            <span class="hot-item__rank font-display" aria-hidden="true">{{ i + 1 }}</span>
            <div class="hot-item__body">
              <p class="hot-item__title">{{ post.title }}</p>
              <span class="hot-item__heat">{{ formatCompact(post.viewCount) }} 热度</span>
            </div>
          </RouterLink>
        </section>

        <section class="rail-section">
          <h3 class="rail-section__title font-mono">热门话题</h3>
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

        <section v-if="keepers.length" class="rail-section">
          <h3 class="rail-section__title font-mono">活跃掌柜</h3>
          <ul class="member-list">
            <li v-for="m in keepers" :key="m.key" class="member">
              <span class="member__avatar" aria-hidden="true">{{ m.initial }}</span>
              <div class="member__body">
                <b class="member__name">{{ m.name }}</b>
                <span class="member__sub">{{ m.subtitle }}</span>
              </div>
              <GsButton variant="text" class="member__follow">
                {{ m.following ? '已关注' : '关注' }}
              </GsButton>
            </li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
}

.home-hero {
  margin: 0 0 8px;
  padding: clamp(40px, 6vh, 72px) 0 48px;
  border-bottom: 1px solid var(--border);
}

.home-hero__kicker {
  margin: 0 0 18px;
  font-size: 0.78rem;
  color: var(--text-3);
}

.home-hero__lead {
  margin: 20px 0 0;
  max-width: 44ch;
  font-size: 1.05rem;
  line-height: 1.7;
  color: var(--text-2);
}

.home-hero__meta {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 28px;
}

.home-hero__facts {
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}

.home-hero__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  margin-left: auto;
}

.home-grid {
  padding: 48px 0 72px;
}

.home-error {
  padding: 48px 0;
}

.feed-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.feed-head__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.333rem;
  letter-spacing: -0.01em;
}

.home-side {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.rail-section__title {
  margin: 0 0 14px;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-3);
  display: flex;
  align-items: center;
  gap: 8px;
}

.rail-section__title::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.hot-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
  text-decoration: none;
  color: inherit;
}

.hot-item:last-child {
  border-bottom: none;
}

.hot-item__rank {
  font-weight: 700;
  font-size: 0.95rem;
  color: var(--text-3);
  min-width: 20px;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.hot-item__title {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--text-1);
  transition: color 0.15s var(--ease-out);
}

.hot-item:hover .hot-item__title {
  color: var(--accent);
}

.hot-item__heat {
  display: block;
  margin-top: 2px;
  font-size: 0.72rem;
  color: var(--text-3);
}

.topic-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.topic-chips a {
  text-decoration: none;
}

.member-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--surface-3);
  display: grid;
  place-items: center;
  font-size: 0.7rem;
  color: var(--text-2);
  flex-shrink: 0;
}

.member__body {
  min-width: 0;
  flex: 1;
}

.member__name {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-1);
}

.member__sub {
  display: block;
  font-size: 0.72rem;
  color: var(--text-3);
}

.member__follow {
  margin-left: auto;
  flex-shrink: 0;
  min-height: 36px;
  padding: 4px 6px;
}

@media (max-width: 560px) {
  .home-hero {
    padding: 36px 0 40px;
  }

  .home-hero__facts {
    display: none;
  }

  .home-grid {
    padding: 32px 0 48px;
  }
}
</style>

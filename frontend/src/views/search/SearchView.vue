<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NEmpty,
  NIcon,
  NInput,
  NPagination,
  NSpace,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
  NText
} from 'naive-ui'
import {
  ChatbubblesOutline,
  CubeOutline,
  PricetagOutline,
  SearchOutline
} from '@vicons/ionicons5'
import { fetchCategories, type Category, type PostCard } from '@/api/forum'
import type { ShareCard } from '@/api/share'
import { searchContent, type SearchType } from '@/api/search'

const PAGE_SIZE = 10
const DEBOUNCE_MS = 350

const route = useRoute()
const router = useRouter()

const qInput = ref('')
/** 已提交搜索的关键词（用于结果区回显；Vue 文本插值自动转义） */
const qActive = ref('')
const searchType = ref<SearchType>('post')
const loading = ref(false)
const searched = ref(false)
const total = ref(0)
const current = ref(1)
const postRecords = ref<PostCard[]>([])
const shareRecords = ref<ShareCard[]>([])
const categories = ref<Category[]>([])

let debounceTimer: ReturnType<typeof setTimeout> | null = null
let requestSeq = 0
/** 挂载完成后再响应输入防抖，避免 URL 回显触发二次请求 */
let inputReady = false

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

function syncRoute(q: string, type: SearchType, page: number) {
  void router.replace({
    name: 'search',
    query: {
      q: q || undefined,
      type,
      page: page > 1 ? String(page) : undefined
    }
  })
}

async function runSearch(page = current.value) {
  const q = qInput.value.trim()
  qActive.value = q
  searched.value = true
  current.value = page
  syncRoute(q, searchType.value, page)

  if (!q) {
    postRecords.value = []
    shareRecords.value = []
    total.value = 0
    loading.value = false
    return
  }

  const seq = ++requestSeq
  loading.value = true
  try {
    const pageData = await searchContent(q, searchType.value, page, PAGE_SIZE)
    if (seq !== requestSeq) return
    total.value = pageData.total
    if (searchType.value === 'post') {
      postRecords.value = pageData.records as PostCard[]
      shareRecords.value = []
    } else {
      shareRecords.value = pageData.records as ShareCard[]
      postRecords.value = []
    }
  } catch {
    if (seq !== requestSeq) return
    postRecords.value = []
    shareRecords.value = []
    total.value = 0
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

function scheduleSearch(resetPage = true) {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    void runSearch(resetPage ? 1 : current.value)
  }, DEBOUNCE_MS)
}

function onTabChange(name: string | number) {
  searchType.value = name === 'share' ? 'share' : 'post'
  void runSearch(1)
}

function onPageChange(page: number) {
  void runSearch(page)
}

watch(qInput, () => {
  if (!inputReady) return
  scheduleSearch(true)
})

onMounted(async () => {
  const q = typeof route.query.q === 'string' ? route.query.q : ''
  const type = route.query.type === 'share' ? 'share' : 'post'
  const page = Math.max(1, Number(route.query.page) || 1)
  qInput.value = q
  searchType.value = type
  current.value = page
  try {
    categories.value = await fetchCategories()
  } catch {
    categories.value = []
  }
  if (q.trim()) {
    await runSearch(page)
  } else {
    searched.value = false
  }
  inputReady = true
})
</script>

<template>
  <div class="search-page">
    <div class="search-header">
      <h2 class="gs-section-title">
        <n-icon :component="SearchOutline" :size="22" />
        搜索
      </h2>
      <RouterLink class="search-tags-link" to="/tags">
        <n-icon :component="PricetagOutline" :size="16" />
        浏览标签
      </RouterLink>
    </div>

    <n-input
      v-model:value="qInput"
      size="large"
      clearable
      placeholder="搜索标题或内容关键字"
      class="search-input"
      @keyup.enter="runSearch(1)"
    >
      <template #prefix>
        <n-icon :component="SearchOutline" />
      </template>
    </n-input>

    <n-tabs
      :value="searchType"
      type="segment"
      class="search-tabs"
      @update:value="onTabChange"
    >
      <n-tab-pane name="post" tab="帖子" />
      <n-tab-pane name="share" tab="项目" />
    </n-tabs>

    <p v-if="qActive" class="search-echo">
      关键词：<strong>{{ qActive }}</strong>
      <n-text depth="3"> · {{ searchType === 'post' ? '帖子' : '项目' }} · 共 {{ total }} 条</n-text>
    </p>

    <n-spin :show="loading">
      <n-empty
        v-if="!loading && searched && qActive && total === 0"
        description="没有找到相关结果"
      >
        <template #icon>
          <n-icon
            :component="searchType === 'post' ? ChatbubblesOutline : CubeOutline"
            :size="48"
          />
        </template>
        <template #extra>
          <n-space>
            <RouterLink to="/tags">
              <n-button>浏览标签</n-button>
            </RouterLink>
            <RouterLink :to="searchType === 'post' ? '/forum/new' : '/shares/new'">
              <n-button type="primary">{{ searchType === 'post' ? '去发帖' : '去发布' }}</n-button>
            </RouterLink>
          </n-space>
        </template>
      </n-empty>

      <n-empty
        v-else-if="!loading && !qActive"
        description="输入关键字开始搜索"
      >
        <template #icon>
          <n-icon :component="SearchOutline" :size="48" />
        </template>
      </n-empty>

      <!-- 帖子结果 -->
      <div v-else-if="searchType === 'post' && postRecords.length" class="result-list">
        <n-card
          v-for="post in postRecords"
          :key="post.id"
          class="gs-card-elevated result-row"
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

      <!-- 项目结果 -->
      <div v-else-if="searchType === 'share' && shareRecords.length" class="card-grid">
        <RouterLink
          v-for="card in shareRecords"
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

      <div v-if="total > PAGE_SIZE" class="pager">
        <n-pagination
          :page="current"
          :page-size="PAGE_SIZE"
          :item-count="total"
          :disabled="loading"
          @update:page="onPageChange"
        />
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.search-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.search-tags-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: none;
}

.search-tags-link:hover {
  text-decoration: underline;
}

.search-input {
  max-width: 640px;
}

.search-tabs {
  max-width: 320px;
}

.search-echo {
  margin: 0;
  font-size: 13px;
  color: var(--color-text);
}

.search-echo strong {
  color: var(--color-primary);
  font-weight: 600;
  word-break: break-word;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-row {
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
}

.row-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 6px;
  font-size: 12px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
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

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>

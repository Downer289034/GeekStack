<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { NEmpty, NIcon, NInput, NPagination } from 'naive-ui'
import {
  ChatbubblesOutline,
  CubeOutline,
  PricetagOutline,
  SearchOutline
} from '@vicons/ionicons5'
import { fetchCategories, type Category, type PostCard } from '@/api/forum'
import type { ShareCard } from '@/api/share'
import { searchContent, type SearchType } from '@/api/search'
import GsButton from '@/components/gs/GsButton.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsProjectCard from '@/components/gs/GsProjectCard.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsUnderlineTabs from '@/components/gs/GsUnderlineTabs.vue'

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

const typeTabs = [
  { key: 'post', label: '帖子' },
  { key: 'share', label: '项目' }
]

const typeModel = computed({
  get: () => searchType.value,
  set: (v: string) => {
    if (v === 'post' || v === 'share') {
      searchType.value = v
      void runSearch(1)
    }
  }
})

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
      <h2 class="search-title font-display">
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

    <GsUnderlineTabs v-model="typeModel" :items="typeTabs" aria-label="搜索类型" />

    <p v-if="qActive" class="search-echo font-mono">
      关键词：<strong>{{ qActive }}</strong>
      <span> · {{ searchType === 'post' ? '帖子' : '项目' }} · 共 {{ total }} 条</span>
    </p>

    <GsSkeleton
      v-if="loading"
      :variant="searchType === 'share' ? 'card' : 'row'"
      :count="searchType === 'share' ? 6 : 6"
    />

    <template v-else>
      <n-empty
        v-if="searched && qActive && total === 0"
        description="没有找到相关结果"
      >
        <template #icon>
          <n-icon
            :component="searchType === 'post' ? ChatbubblesOutline : CubeOutline"
            :size="48"
          />
        </template>
        <template #extra>
          <div class="empty-actions">
            <GsButton variant="ghost" to="/tags">浏览标签</GsButton>
            <GsButton
              variant="primary"
              :to="searchType === 'post' ? '/forum/new' : '/shares/new'"
            >
              {{ searchType === 'post' ? '去发帖' : '去发布' }}
            </GsButton>
          </div>
        </template>
      </n-empty>

      <n-empty v-else-if="!qActive" description="输入关键字开始搜索">
        <template #icon>
          <n-icon :component="SearchOutline" :size="48" />
        </template>
      </n-empty>

      <div v-else-if="searchType === 'post' && postRecords.length" class="result-list">
        <GsFeedRow
          v-for="post in postRecords"
          :key="post.id"
          v-reveal
          :title="post.title"
          :to="`/forum/posts/${post.id}`"
          :tags="[categoryLabel(post)]"
          :pinned="post.isPinned === 1"
          :meta="`回帖 ${post.replyCount} · 浏览 ${post.viewCount} · ${formatTime(post.createdAt)}`"
        />
      </div>

      <div v-else-if="searchType === 'share' && shareRecords.length" class="card-grid">
        <GsProjectCard
          v-for="(card, index) in shareRecords"
          :key="card.id"
          v-reveal
          :title="card.title"
          :to="`/shares/${card.id}`"
          :tags="card.tags ?? []"
          :view-count="card.viewCount"
          :time="formatTime(card.createdAt)"
          :featured="current === 1 && index === 0"
        />
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
    </template>
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

.search-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.search-tags-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--accent);
  text-decoration: none;
}

.search-tags-link:hover {
  text-decoration: underline;
}

.search-input {
  max-width: 640px;
}

.search-echo {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-2);
}

.search-echo strong {
  color: var(--accent);
  font-weight: 600;
  word-break: break-word;
}

.search-echo span {
  color: var(--text-3);
}

.empty-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.result-list {
  margin-top: 4px;
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

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>

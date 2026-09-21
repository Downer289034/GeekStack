<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NEmpty, NIcon, NInput, NPagination, NSpace } from 'naive-ui'
import { AddOutline, ChatbubblesOutline, CreateOutline, SearchOutline } from '@vicons/ionicons5'
import { fetchCategories, pagePosts, type Category, type PostCard } from '@/api/forum'
import GsButton from '@/components/gs/GsButton.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsUnderlineTabs from '@/components/gs/GsUnderlineTabs.vue'

const PAGE_SIZE = 12

const route = useRoute()
const router = useRouter()

const categories = ref<Category[]>([])
const activeCategory = ref('')
const sortBy = ref<'latest' | 'hot'>('latest')
const tagInput = ref('')
const activeTag = ref('')
const posts = ref<PostCard[]>([])
const total = ref(0)
const current = ref(1)
const loading = ref(true)

const categoryById = computed(() => {
  const map = new Map<number, Category>()
  for (const cat of categories.value) {
    map.set(cat.id, cat)
  }
  return map
})

const categoryTabs = computed(() => [
  { key: '', label: '全部' },
  ...categories.value.map((c) => ({ key: c.slug, label: c.name }))
])

const sortTabs = [
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '最热' }
]

const sortModel = computed({
  get: () => sortBy.value,
  set: (v: string) => {
    if (v === 'latest' || v === 'hot') sortBy.value = v
  }
})

function categoryLabel(post: PostCard): string {
  return categoryById.value.get(post.categoryId)?.name ?? '讨论'
}

function rowTags(post: PostCard): string[] {
  return [categoryLabel(post), ...(post.tags ?? [])]
}

function postMeta(post: PostCard): string {
  return `回帖 ${post.replyCount} · 浏览 ${post.viewCount} · 最后回复 ${formatTime(post.lastReplyAt)}`
}

function syncTagQuery(tag: string) {
  const next = { ...route.query } as Record<string, string | string[] | undefined>
  if (tag) {
    next.tag = tag
  } else {
    delete next.tag
  }
  void router.replace({ query: next })
}

async function load() {
  loading.value = true
  try {
    const page = await pagePosts(
      current.value,
      PAGE_SIZE,
      activeCategory.value || undefined,
      sortBy.value,
      activeTag.value || undefined
    )
    posts.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function applyTagFilter() {
  activeTag.value = tagInput.value.trim()
  current.value = 1
  syncTagQuery(activeTag.value)
  void load()
}

function clearTagFilter() {
  tagInput.value = ''
  activeTag.value = ''
  current.value = 1
  syncTagQuery('')
  void load()
}

function formatTime(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

/** 热榜前三徽标：仅最热排序 + 第一页前三项 */
function hotRank(index: number): number | null {
  if (sortBy.value !== 'hot' || current.value !== 1 || index >= 3) return null
  return index + 1
}

watch([activeCategory, sortBy], () => {
  current.value = 1
  void load()
})
watch(current, () => {
  void load()
})

onMounted(async () => {
  const qTag = typeof route.query.tag === 'string' ? route.query.tag.trim() : ''
  if (qTag) {
    tagInput.value = qTag
    activeTag.value = qTag
  }
  try {
    categories.value = await fetchCategories()
  } finally {
    void load()
  }
})
</script>

<template>
  <div class="forum">
    <div class="forum-header">
      <h2 class="forum-title font-display">
        <n-icon :component="ChatbubblesOutline" :size="22" />
        技术论坛
      </h2>
      <n-space align="center" :wrap="true">
        <n-input
          v-model:value="tagInput"
          placeholder="按标签筛选"
          size="small"
          style="width: 180px"
          @keyup.enter="applyTagFilter"
        >
          <template #prefix>
            <n-icon :component="SearchOutline" />
          </template>
        </n-input>
        <GsButton variant="ghost" @click="applyTagFilter">筛选</GsButton>
        <GsButton v-if="activeTag" variant="text" @click="clearTagFilter">清除</GsButton>
        <GsButton variant="ghost" to="/forum/new">
          <n-icon :component="AddOutline" :size="16" />
          发帖
        </GsButton>
      </n-space>
    </div>

    <div class="forum-filter">
      <GsUnderlineTabs
        v-model="activeCategory"
        :items="categoryTabs"
        aria-label="论坛分类"
      />
      <GsUnderlineTabs
        v-model="sortModel"
        :items="sortTabs"
        aria-label="排序"
      />
    </div>

    <GsSkeleton v-if="loading" variant="row" :count="8" />

    <n-empty
      v-else-if="posts.length === 0"
      description="还没有帖子，来发第一帖"
      class="forum-empty"
    >
      <template #icon>
        <n-icon :component="CreateOutline" :size="48" />
      </template>
      <template #extra>
        <GsButton variant="primary" to="/forum/new">去发帖</GsButton>
      </template>
    </n-empty>

    <div v-else class="forum-list">
      <GsFeedRow
        v-for="(post, index) in posts"
        :key="post.id"
        v-reveal
        :title="post.title"
        :to="`/forum/posts/${post.id}`"
        :tags="rowTags(post)"
        :pinned="post.isPinned === 1"
        :featured="post.isFeatured === 1"
        :rank="hotRank(index)"
        :show-rank="hotRank(index) != null"
        :meta="postMeta(post)"
      />
    </div>

    <div class="forum-pagination">
      <n-pagination
        :page="current"
        :page-size="PAGE_SIZE"
        :item-count="total"
        :disabled="loading"
        @update:page="(p: number) => (current = p)"
      />
    </div>
  </div>
</template>

<style scoped>
.forum-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.forum-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.forum-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--border);
}

.forum-empty {
  margin: 64px 0;
}

.forum-list {
  margin-top: 4px;
}

.forum-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

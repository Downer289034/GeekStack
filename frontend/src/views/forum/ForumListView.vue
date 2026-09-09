<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  NButton,
  NCard,
  NEmpty,
  NPagination,
  NTag,
  NText
} from 'naive-ui'
import { fetchCategories, pagePosts, type Category, type PostCard } from '@/api/forum'

const PAGE_SIZE = 12

const categories = ref<Category[]>([])
const activeCategory = ref('')
const sortBy = ref<'latest' | 'hot'>('latest')
const posts = ref<PostCard[]>([])
const total = ref(0)
const current = ref(1)
const loading = ref(false)

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

async function load() {
  loading.value = true
  try {
    const page = await pagePosts(current.value, PAGE_SIZE, activeCategory.value || undefined, sortBy.value)
    posts.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function pickCategory(slug: string) {
  activeCategory.value = slug
  current.value = 1
}

function pickSort(sort: 'latest' | 'hot') {
  sortBy.value = sort
  current.value = 1
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
  void load()
})
watch(current, () => {
  void load()
})

onMounted(async () => {
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
      <h2 class="forum-title">技术论坛</h2>
      <RouterLink to="/forum/new">
        <n-button size="small" type="primary">发帖</n-button>
      </RouterLink>
    </div>

    <div class="forum-filter">
      <div class="cat-row">
        <button
          class="cat-chip"
          :class="{ active: activeCategory === '' }"
          @click="pickCategory('')"
        >
          全部
        </button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          class="cat-chip"
          :class="{ active: activeCategory === cat.slug }"
          @click="pickCategory(cat.slug)"
        >
          {{ cat.name }}
        </button>
      </div>
      <div class="sort-row">
        <button
          class="sort-chip"
          :class="{ active: sortBy === 'latest' }"
          @click="pickSort('latest')"
        >
          最新
        </button>
        <button
          class="sort-chip"
          :class="{ active: sortBy === 'hot' }"
          @click="pickSort('hot')"
        >
          最热
        </button>
      </div>
    </div>

    <n-empty v-if="!loading && posts.length === 0" description="还没有帖子，来发第一帖" class="forum-empty" />

    <n-card
      v-for="(post, index) in posts"
      :key="post.id"
      class="gs-card-elevated post-row"
      :bordered="false"
    >
      <RouterLink :to="`/forum/posts/${post.id}`" class="post-row-link">
        <div class="post-row-main">
          <span v-if="hotRank(index) != null" class="gs-hot-rank">{{ hotRank(index) }}</span>
          <n-tag v-if="post.isPinned === 1" size="small" type="warning" :bordered="false">置顶</n-tag>
          <n-tag class="gs-tag-forum" size="small" :bordered="false">{{ categoryLabel(post) }}</n-tag>
          <span class="post-row-title">{{ post.title }}</span>
        </div>
        <div class="post-row-meta">
          <n-text depth="3">回帖 {{ post.replyCount }}</n-text>
          <n-text depth="3">浏览 {{ post.viewCount }}</n-text>
          <n-text depth="3">最后回复 {{ formatTime(post.lastReplyAt) }}</n-text>
        </div>
      </RouterLink>
    </n-card>

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
  margin-bottom: 16px;
}

.forum-title {
  margin: 0;
  font-size: 20px;
}

.forum-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 16px;
}

.cat-row,
.sort-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cat-chip,
.sort-chip {
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text);
  border-radius: 999px;
  padding: 4px 14px;
  font-size: 13px;
  cursor: pointer;
}

.cat-chip.active,
.sort-chip.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-bg-container);
}

.forum-empty {
  margin: 64px 0;
}

.post-row {
  margin-bottom: 12px;
}

.post-row-link {
  text-decoration: none;
  display: block;
  min-width: 0;
}

.post-row-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.post-row-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  min-width: 0;
}

.post-row-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 6px;
  font-size: 12px;
}

.forum-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

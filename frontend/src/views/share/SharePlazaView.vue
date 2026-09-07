<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  NButton,
  NCard,
  NEmpty,
  NInput,
  NPagination,
  NSpace,
  NTag,
  NText
} from 'naive-ui'
import { pageShares, type ShareCard } from '@/api/share'

const PAGE_SIZE = 9

const loading = ref(false)
const cards = ref<ShareCard[]>([])
const total = ref(0)
const current = ref(1)
const tagInput = ref('')
const activeTag = ref('')

async function load() {
  loading.value = true
  try {
    const page = await pageShares(current.value, PAGE_SIZE, activeTag.value || undefined)
    cards.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function applyTagFilter() {
  activeTag.value = tagInput.value.trim()
  current.value = 1
  void load()
}

function clearTagFilter() {
  tagInput.value = ''
  activeTag.value = ''
  current.value = 1
  void load()
}

function onPageChange(page: number) {
  current.value = page
  void load()
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="plaza">
    <div class="plaza-header">
      <h2 class="plaza-title">项目广场</h2>
      <n-space>
        <n-input
          v-model:value="tagInput"
          placeholder="按标签筛选"
          size="small"
          style="width: 160px"
          @keyup.enter="applyTagFilter"
        />
        <n-button size="small" @click="applyTagFilter">筛选</n-button>
        <n-button v-if="activeTag" size="small" quaternary @click="clearTagFilter">清除</n-button>
      </n-space>
    </div>

    <n-empty
      v-if="!loading && cards.length === 0"
      description="暂无项目分享，来发布第一个吧"
      class="plaza-empty"
    />

    <div class="card-grid">
      <RouterLink
        v-for="card in cards"
        :key="card.id"
        :to="`/shares/${card.id}`"
        class="card-link"
      >
        <n-card hoverable>
          <div class="card-title-row">
            <span class="card-title">{{ card.title }}</span>
            <n-tag size="small" :bordered="false">浏览 {{ card.viewCount }}</n-tag>
          </div>
          <p class="card-summary">{{ card.summary }}</p>
          <div class="card-footer">
            <n-space size="small">
              <n-tag v-for="t in card.tags ?? []" :key="t" size="small" type="info" :bordered="false">
                {{ t }}
              </n-tag>
            </n-space>
            <n-text depth="3" class="card-time">{{ formatTime(card.createdAt) }}</n-text>
          </div>
        </n-card>
      </RouterLink>
    </div>

    <div class="plaza-pagination">
      <n-pagination
        :page="current"
        :page-size="PAGE_SIZE"
        :item-count="total"
        :disabled="loading"
        @update:page="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.plaza-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.plaza-title {
  margin: 0;
  font-size: 20px;
}

.plaza-empty {
  margin: 64px 0;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.card-link {
  text-decoration: none;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.card-summary {
  margin: 8px 0 12px;
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
}

.card-time {
  font-size: 12px;
  white-space: nowrap;
}

.plaza-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

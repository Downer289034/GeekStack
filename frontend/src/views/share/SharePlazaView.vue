<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  NButton,
  NCard,
  NEmpty,
  NIcon,
  NInput,
  NPagination,
  NSpace,
  NTag,
  NText
} from 'naive-ui'
import { CubeOutline, GridOutline, SearchOutline } from '@vicons/ionicons5'
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

/** 封面占位色相：按 id 在分区青系内轻微轮换，避免整页同色块 */
function coverTone(id: number): string {
  const tones = [0, 8, 16]
  return `color-mix(in srgb, var(--color-share) ${18 + tones[id % 3]}%, var(--color-bg-container))`
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="plaza">
    <div class="plaza-header">
      <h2 class="gs-section-title">
        <n-icon :component="GridOutline" :size="22" />
        项目广场
      </h2>
      <n-space>
        <n-input
          v-model:value="tagInput"
          placeholder="按标签筛选"
          size="small"
          style="width: 160px"
          @keyup.enter="applyTagFilter"
        >
          <template #prefix>
            <n-icon :component="SearchOutline" />
          </template>
        </n-input>
        <n-button size="small" @click="applyTagFilter">筛选</n-button>
        <n-button v-if="activeTag" size="small" quaternary @click="clearTagFilter">清除</n-button>
      </n-space>
    </div>

    <n-empty
      v-if="!loading && cards.length === 0"
      description="暂无项目分享，来发布第一个吧"
      class="plaza-empty"
    >
      <template #icon>
        <n-icon :component="CubeOutline" :size="48" />
      </template>
      <template #extra>
        <RouterLink to="/shares/new">
          <n-button type="primary">去发布</n-button>
        </RouterLink>
      </template>
    </n-empty>

    <div v-else class="card-grid">
      <RouterLink
        v-for="card in cards"
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
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 16px;
}

.plaza-empty {
  margin: 64px 0;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* 断点口径见 tokens.css：1200↔768 过渡带 2 列，≤768 单列 */
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
  height: 112px;
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
  padding: 14px 16px 16px;
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
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  flex-wrap: wrap;
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

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NEmpty, NIcon, NInput, NPagination, NSpace } from 'naive-ui'
import { CubeOutline, GridOutline, SearchOutline } from '@vicons/ionicons5'
import { pageShares, type ShareCard } from '@/api/share'
import GsButton from '@/components/gs/GsButton.vue'
import GsProjectCard from '@/components/gs/GsProjectCard.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'

const PAGE_SIZE = 9
const PLAZA_SUBTITLE = '探索开源 AI 项目、Agent 框架、向量数据库与 LLM 工具库'

const loading = ref(true)
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
    <header class="plaza-header">
      <div class="plaza-header__left">
        <h2 class="plaza-title font-display">
          <n-icon :component="GridOutline" :size="22" />
          项目广场
        </h2>
        <p class="plaza-subtitle">{{ PLAZA_SUBTITLE }}</p>
      </div>
      <div class="plaza-header__right">
        <span class="plaza-count font-mono">共 {{ total }} 个项目</span>
        <n-space align="center" :wrap="true" :size="8">
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
        </n-space>
      </div>
    </header>

    <GsSkeleton v-if="loading" variant="card" :count="6" />

    <n-empty
      v-else-if="cards.length === 0"
      description="暂无项目分享，来发布第一个吧"
      class="plaza-empty"
    >
      <template #icon>
        <n-icon :component="CubeOutline" :size="48" />
      </template>
      <template #extra>
        <GsButton variant="primary" to="/shares/new">去发布</GsButton>
      </template>
    </n-empty>

    <div v-else class="card-grid">
      <GsProjectCard
        v-for="card in cards"
        :key="card.id"
        v-reveal
        :title="card.title"
        :to="`/shares/${card.id}`"
        :summary="card.summary"
        :tags="card.tags ?? []"
        :view-count="card.viewCount"
        :time="formatTime(card.createdAt)"
      />
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
.plaza {
  position: relative;
}

/* 克制网格纹理，不抢内容 */
.plaza::before {
  content: '';
  pointer-events: none;
  position: absolute;
  inset: -8px 0 0;
  z-index: 0;
  opacity: 0.35;
  background-image:
    linear-gradient(color-mix(in oklch, var(--border) 55%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in oklch, var(--border) 55%, transparent) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(180deg, var(--bg) 0%, transparent 72%);
}

.plaza > * {
  position: relative;
  z-index: 1;
}

.plaza-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px 24px;
  margin-bottom: 24px;
}

.plaza-header__left {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.plaza-header__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.plaza-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--text-1);
}

.plaza-subtitle {
  margin: 0;
  max-width: 36rem;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--text-3);
}

.plaza-count {
  font-size: 0.75rem;
  color: var(--text-3);
}

.plaza-empty {
  margin: 64px 0;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 22px;
}

@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .plaza-header__right {
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

.plaza-pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>

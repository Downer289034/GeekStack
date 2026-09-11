<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NEmpty, NIcon, NInput, NPagination, NSpace } from 'naive-ui'
import { CubeOutline, GridOutline, SearchOutline } from '@vicons/ionicons5'
import { pageShares, type ShareCard } from '@/api/share'
import GsButton from '@/components/gs/GsButton.vue'
import GsProjectCard from '@/components/gs/GsProjectCard.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'

const PAGE_SIZE = 9

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
    <div class="plaza-header">
      <h2 class="plaza-title font-display">
        <n-icon :component="GridOutline" :size="22" />
        项目广场
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
      </n-space>
    </div>

    <div class="plaza-toolbar">
      <span class="plaza-count font-mono">共 {{ total }} 个项目</span>
    </div>

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
        v-for="(card, index) in cards"
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
  margin-bottom: 12px;
}

.plaza-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.plaza-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 16px;
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

.plaza-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NEmpty,
  NIcon,
  NPagination,
  NSpace,
  NSpin,
  NTag,
  NText
} from 'naive-ui'
import { ArrowBackOutline, CubeOutline, PricetagOutline } from '@vicons/ionicons5'
import { pageShares, type ShareCard } from '@/api/share'

const PAGE_SIZE = 9
const route = useRoute()
const router = useRouter()

const tagName = computed(() => decodeURIComponent(String(route.params.name ?? '')))

const loading = ref(false)
const cards = ref<ShareCard[]>([])
const total = ref(0)
const current = ref(1)

function coverTone(id: number): string {
  const tones = [0, 8, 16]
  return `color-mix(in srgb, var(--color-share) ${18 + tones[id % 3]}%, var(--color-bg-container))`
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  if (!tagName.value) return
  loading.value = true
  try {
    const page = await pageShares(current.value, PAGE_SIZE, tagName.value)
    cards.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function onPageChange(page: number) {
  current.value = page
  void load()
}

watch(
  () => route.params.name,
  () => {
    current.value = 1
    void load()
  }
)

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="tag-detail">
    <div class="tag-detail__header">
      <div class="tag-detail__title-row">
        <n-button quaternary size="small" @click="router.push({ name: 'tags' })">
          <template #icon>
            <n-icon :component="ArrowBackOutline" />
          </template>
          标签云
        </n-button>
        <h2 class="gs-section-title">
          <n-icon :component="PricetagOutline" :size="22" />
          <n-tag class="gs-tag-share" size="medium" :bordered="false">{{ tagName }}</n-tag>
        </h2>
      </div>
      <n-text depth="3">共 {{ total }} 个项目</n-text>
    </div>

    <n-spin :show="loading">
      <n-empty
        v-if="!loading && cards.length === 0"
        description="该标签下暂无项目"
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

      <div class="pager">
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
.tag-detail__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.tag-detail__title-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
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

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NEmpty, NIcon, NPagination } from 'naive-ui'
import { CubeOutline, PricetagOutline } from '@vicons/ionicons5'
import { pageShares, type ShareCard } from '@/api/share'
import GsButton from '@/components/gs/GsButton.vue'
import GsProjectCard from '@/components/gs/GsProjectCard.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsTag from '@/components/gs/GsTag.vue'

const PAGE_SIZE = 9
const route = useRoute()
const router = useRouter()

const tagName = computed(() => decodeURIComponent(String(route.params.name ?? '')))

const loading = ref(true)
const cards = ref<ShareCard[]>([])
const total = ref(0)
const current = ref(1)

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
        <GsButton variant="text" @click="router.push({ name: 'tags' })">← 标签云</GsButton>
        <h2 class="tag-detail__title font-display">
          <n-icon :component="PricetagOutline" :size="22" />
          <GsTag>{{ tagName }}</GsTag>
        </h2>
      </div>
      <span class="tag-detail__count font-mono">共 {{ total }} 个项目</span>
    </div>

    <GsSkeleton v-if="loading" variant="card" :count="6" />

    <template v-else>
      <n-empty v-if="cards.length === 0" description="该标签下暂无项目">
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

      <div class="pager">
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

.tag-detail__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.tag-detail__count {
  font-size: 0.75rem;
  color: var(--text-3);
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
  margin-top: 24px;
}
</style>

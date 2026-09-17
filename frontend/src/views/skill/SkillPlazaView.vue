<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NEmpty, NIcon, NInput, NPagination, NSpace } from 'naive-ui'
import { AddOutline, FlashOutline, SearchOutline } from '@vicons/ionicons5'
import { pageSkills, type SkillCard } from '@/api/skill'
import GsButton from '@/components/gs/GsButton.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'

const PAGE_SIZE = 12

const loading = ref(true)
const cards = ref<SkillCard[]>([])
const total = ref(0)
const current = ref(1)
const tagInput = ref('')
const activeTag = ref('')

async function load() {
  loading.value = true
  try {
    const page = await pageSkills(current.value, PAGE_SIZE, activeTag.value || undefined)
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

function skillMeta(card: SkillCard): string {
  const parts = [`浏览 ${card.viewCount}`, formatTime(card.createdAt)]
  if (card.likeCount != null) parts.unshift(`赞 ${card.likeCount}`)
  return parts.join(' · ')
}

function modelExcerpt(card: SkillCard): string {
  const summary = card.summary?.trim()
  const notes = card.modelAdaptationMd?.trim()
  if (summary && notes) return `${summary}\n模型适配：${notes}`
  if (notes) return `模型适配：${notes}`
  return summary ?? ''
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="plaza">
    <div class="plaza-header">
      <h2 class="plaza-title font-display">
        <n-icon :component="FlashOutline" :size="22" />
        Skill 广场
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
        <GsButton variant="ghost" to="/skills/new">
          <n-icon :component="AddOutline" :size="16" />
          发布 Skill
        </GsButton>
      </n-space>
    </div>

    <div class="plaza-toolbar">
      <span class="plaza-count font-mono">共 {{ total }} 个 Skill</span>
    </div>

    <GsSkeleton v-if="loading" variant="row" :count="8" />

    <n-empty
      v-else-if="cards.length === 0"
      description="暂无 Skill，来发布第一个提示词包吧"
      class="plaza-empty"
    >
      <template #icon>
        <n-icon :component="FlashOutline" :size="48" />
      </template>
      <template #extra>
        <GsButton variant="primary" to="/skills/new">去发布</GsButton>
      </template>
    </n-empty>

    <div v-else class="plaza-list">
      <GsFeedRow
        v-for="card in cards"
        :key="card.id"
        v-reveal
        :title="card.title"
        :to="`/skills/${card.id}`"
        :tags="card.tags ?? []"
        :excerpt="modelExcerpt(card)"
        :meta="skillMeta(card)"
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

.plaza-list {
  margin-top: 4px;
}

.plaza-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

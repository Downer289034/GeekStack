<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { NButton, NEmpty, NIcon, NSpin } from 'naive-ui'
import { CubeOutline, PricetagOutline, SearchOutline } from '@vicons/ionicons5'
import { fetchTagCloud, type TagCount } from '@/api/search'

const loading = ref(true)
const tags = ref<TagCount[]>([])
const loadError = ref(false)

const maxCount = computed(() => (tags.value.length ? Math.max(...tags.value.map((t) => t.count)) : 1))
const minCount = computed(() => (tags.value.length ? Math.min(...tags.value.map((t) => t.count)) : 1))

function tagStyle(count: number): Record<string, string> {
  const max = maxCount.value
  const min = minCount.value
  const t = max === min ? 1 : (count - min) / (max - min)
  const fontSize = 13 + Math.round(t * 13)
  const opacity = (0.55 + t * 0.45).toFixed(2)
  return {
    fontSize: `${fontSize}px`,
    opacity,
    color: 'var(--color-share-text)'
  }
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    tags.value = await fetchTagCloud()
  } catch {
    loadError.value = true
    tags.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="tags-page">
    <div class="tags-header">
      <h2 class="gs-section-title">
        <n-icon :component="PricetagOutline" :size="22" />
        项目标签
      </h2>
      <RouterLink class="tags-to-search" to="/search?type=share">
        <n-icon :component="SearchOutline" :size="16" />
        搜索项目
      </RouterLink>
    </div>

    <p class="tags-hint">当前为项目分享标签（share-only）；帖子标签待后端字段就绪后再扩展。</p>

    <n-spin :show="loading">
      <n-empty v-if="loadError" description="标签云加载失败">
        <template #extra>
          <n-button type="primary" @click="load">重试</n-button>
        </template>
      </n-empty>
      <n-empty
        v-else-if="!loading && tags.length === 0"
        description="暂无标签，去发布带标签的项目吧"
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
      <div v-else class="tag-cloud">
        <RouterLink
          v-for="tag in tags"
          :key="tag.name"
          class="tag-cloud__item"
          :to="`/tags/${encodeURIComponent(tag.name)}`"
          :style="tagStyle(tag.count)"
          :title="`${tag.name} · ${tag.count}`"
        >
          {{ tag.name }}
          <span class="tag-cloud__count">{{ tag.count }}</span>
        </RouterLink>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.tags-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tags-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.tags-to-search {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: none;
}

.tags-to-search:hover {
  text-decoration: underline;
}

.tags-hint {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  padding: 20px 0 8px;
  align-items: baseline;
}

.tag-cloud__item {
  text-decoration: none;
  font-weight: 600;
  line-height: 1.3;
  transition: transform 0.15s ease;
}

.tag-cloud__item:hover {
  transform: translateY(-1px);
  text-decoration: underline;
}

.tag-cloud__count {
  margin-left: 4px;
  font-size: 0.75em;
  font-weight: 500;
  opacity: 0.75;
}
</style>

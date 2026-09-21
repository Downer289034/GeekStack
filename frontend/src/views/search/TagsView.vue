<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { NEmpty, NIcon } from 'naive-ui'
import { CubeOutline, PricetagOutline, SearchOutline } from '@vicons/ionicons5'
import { fetchTagCloud, type TagCount } from '@/api/search'
import GsButton from '@/components/gs/GsButton.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'

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
    color: 'var(--accent)'
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
      <h2 class="tags-title font-display">
        <n-icon :component="PricetagOutline" :size="22" />
        标签云
      </h2>
      <RouterLink class="tags-to-search" to="/search">
        <n-icon :component="SearchOutline" :size="16" />
        找内容
      </RouterLink>
    </div>

    <p class="tags-hint">计数含可见帖子、已发布项目与 Skill。</p>

    <GsSkeleton v-if="loading" variant="row" :count="4" />

    <template v-else>
      <n-empty v-if="loadError" description="标签云加载失败">
        <template #extra>
          <GsButton variant="primary" @click="load">重试</GsButton>
        </template>
      </n-empty>
      <n-empty
        v-else-if="tags.length === 0"
        description="暂无标签，发帖或发布项目 / Skill 时可添加"
      >
        <template #icon>
          <n-icon :component="CubeOutline" :size="48" />
        </template>
        <template #extra>
          <GsButton variant="primary" to="/shares/new">去发布</GsButton>
        </template>
      </n-empty>
      <div v-else v-reveal class="tag-cloud">
        <RouterLink
          v-for="tag in tags"
          :key="tag.name"
          class="tag-cloud__item font-mono"
          :to="`/tags/${encodeURIComponent(tag.name)}`"
          :style="tagStyle(tag.count)"
          :title="`${tag.name} · ${tag.count}`"
        >
          {{ tag.name }}
          <span class="tag-cloud__count">{{ tag.count }}</span>
        </RouterLink>
      </div>
    </template>
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

.tags-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.tags-to-search {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--accent);
  text-decoration: none;
}

.tags-to-search:hover {
  text-decoration: underline;
}

.tags-hint {
  margin: 0;
  font-size: 13px;
  color: var(--text-2);
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
  transition: transform 0.15s var(--ease-out);
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

@media (prefers-reduced-motion: reduce) {
  .tag-cloud__item {
    transition: none;
  }

  .tag-cloud__item:hover {
    transform: none;
  }
}
</style>

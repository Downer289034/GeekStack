<script setup lang="ts">
import { RouterLink } from 'vue-router'
import GsTag from '@/components/gs/GsTag.vue'
import GsBadge from '@/components/gs/GsBadge.vue'

withDefaults(
  defineProps<{
    title: string
    to?: string
    tags?: string[]
    excerpt?: string
    author?: string
    authorInitial?: string
    meta?: string
    pinned?: boolean
    featured?: boolean
  }>(),
  {
    to: undefined,
    tags: undefined,
    excerpt: undefined,
    author: undefined,
    authorInitial: undefined,
    meta: undefined,
    pinned: false,
    featured: false
  }
)
</script>

<template>
  <article class="gs-featured">
    <RouterLink v-if="to" class="gs-featured__inner" :to="to">
      <div v-if="(tags && tags.length) || pinned || featured" class="gs-featured__tags">
        <GsBadge v-if="pinned" tone="pinned">置顶</GsBadge>
        <GsBadge v-if="featured" tone="featured">精华</GsBadge>
        <GsTag v-for="t in tags ?? []" :key="t">{{ t }}</GsTag>
      </div>
      <h3 class="gs-featured__title">{{ title }}</h3>
      <p v-if="excerpt" class="gs-featured__excerpt">{{ excerpt }}</p>
      <div v-if="author || meta" class="gs-featured__byline">
        <span v-if="author" class="gs-featured__avatar" aria-hidden="true">
          {{ authorInitial || author.slice(0, 1) }}
        </span>
        <b v-if="author">{{ author }}</b>
        <span v-if="author && meta" class="gs-featured__dot" aria-hidden="true" />
        <span v-if="meta">{{ meta }}</span>
      </div>
    </RouterLink>
    <div v-else class="gs-featured__inner">
      <div v-if="(tags && tags.length) || pinned || featured" class="gs-featured__tags">
        <GsBadge v-if="pinned" tone="pinned">置顶</GsBadge>
        <GsBadge v-if="featured" tone="featured">精华</GsBadge>
        <GsTag v-for="t in tags ?? []" :key="t">{{ t }}</GsTag>
      </div>
      <h3 class="gs-featured__title">{{ title }}</h3>
      <p v-if="excerpt" class="gs-featured__excerpt">{{ excerpt }}</p>
      <div v-if="author || meta" class="gs-featured__byline">
        <span v-if="author" class="gs-featured__avatar" aria-hidden="true">
          {{ authorInitial || author.slice(0, 1) }}
        </span>
        <b v-if="author">{{ author }}</b>
        <span v-if="author && meta" class="gs-featured__dot" aria-hidden="true" />
        <span v-if="meta">{{ meta }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.gs-featured {
  margin-bottom: 8px;
}

.gs-featured__inner {
  display: block;
  text-decoration: none;
  color: inherit;
  padding: 20px 0 24px;
  border-bottom: 1px solid var(--border);
}

.gs-featured__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.gs-featured__title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  line-height: 1.4;
  color: var(--text-1);
  transition: color 0.15s var(--ease-out);
}

.gs-featured__inner:hover .gs-featured__title {
  color: var(--accent);
}

.gs-featured__excerpt {
  margin: 10px 0 0;
  color: var(--text-2);
  font-size: 0.95rem;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.gs-featured__byline {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.8rem;
  color: var(--text-3);
  flex-wrap: wrap;
}

.gs-featured__byline b {
  color: var(--text-2);
  font-weight: 500;
}

.gs-featured__avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.7rem;
  font-weight: 600;
  background: var(--surface-3);
  color: var(--text-1);
}

.gs-featured__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--text-3);
}
</style>

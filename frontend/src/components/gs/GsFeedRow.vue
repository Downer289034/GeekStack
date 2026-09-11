<script setup lang="ts">
import { RouterLink } from 'vue-router'
import GsTag from '@/components/gs/GsTag.vue'
import GsBadge from '@/components/gs/GsBadge.vue'

withDefaults(
  defineProps<{
    title: string
    to?: string
    rank?: number | string | null
    showRank?: boolean
    tags?: string[]
    excerpt?: string
    meta?: string
    pinned?: boolean
  }>(),
  {
    showRank: false,
    rank: null,
    to: undefined,
    tags: undefined,
    excerpt: undefined,
    meta: undefined,
    pinned: false
  }
)
</script>

<template>
  <article class="gs-feed-row" :class="{ 'is-pinned': pinned }">
    <RouterLink v-if="to" class="gs-feed-row__inner" :to="to">
      <span
        v-if="showRank || rank != null"
        class="gs-feed-row__rank font-display"
        aria-hidden="true"
      >
        {{ rank }}
      </span>
      <div class="gs-feed-row__body">
        <div v-if="(tags && tags.length) || pinned" class="gs-feed-row__tags">
          <GsBadge v-if="pinned" tone="pinned">置顶</GsBadge>
          <GsTag v-for="t in tags ?? []" :key="t">{{ t }}</GsTag>
        </div>
        <h3 class="gs-feed-row__title">{{ title }}</h3>
        <p v-if="excerpt" class="gs-feed-row__excerpt">{{ excerpt }}</p>
        <div v-if="meta" class="gs-feed-row__meta font-mono">{{ meta }}</div>
      </div>
    </RouterLink>
    <div v-else class="gs-feed-row__inner">
      <span
        v-if="showRank || rank != null"
        class="gs-feed-row__rank font-display"
        aria-hidden="true"
      >
        {{ rank }}
      </span>
      <div class="gs-feed-row__body">
        <div v-if="(tags && tags.length) || pinned" class="gs-feed-row__tags">
          <GsBadge v-if="pinned" tone="pinned">置顶</GsBadge>
          <GsTag v-for="t in tags ?? []" :key="t">{{ t }}</GsTag>
        </div>
        <h3 class="gs-feed-row__title">{{ title }}</h3>
        <p v-if="excerpt" class="gs-feed-row__excerpt">{{ excerpt }}</p>
        <div v-if="meta" class="gs-feed-row__meta font-mono">{{ meta }}</div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.gs-feed-row {
  border-bottom: 1px solid var(--border);
}

.gs-feed-row.is-pinned {
  background: color-mix(in oklch, var(--accent) 14%, transparent);
  border-radius: var(--radius-small);
  padding-inline: 12px;
  margin-inline: -12px;
  border-bottom-color: transparent;
}

.gs-feed-row__inner {
  display: flex;
  gap: 18px;
  padding: 20px 0;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: transform 0.2s var(--ease-out);
}

.gs-feed-row__inner:hover {
  transform: translateX(8px);
}

.gs-feed-row__rank {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-3);
  min-width: 28px;
  padding-top: 2px;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.gs-feed-row__body {
  min-width: 0;
  flex: 1;
}

.gs-feed-row__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.gs-feed-row__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  line-height: 1.5;
  color: var(--text-1);
  transition: color 0.15s var(--ease-out);
}

.gs-feed-row__inner:hover .gs-feed-row__title {
  color: var(--accent);
}

.gs-feed-row__excerpt {
  margin: 6px 0 0;
  color: var(--text-2);
  font-size: 0.875rem;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.gs-feed-row__meta {
  margin-top: 8px;
  font-size: 0.75rem;
  color: var(--text-3);
}

@media (prefers-reduced-motion: reduce) {
  .gs-feed-row__inner {
    transition: none;
  }

  .gs-feed-row__inner:hover {
    transform: none;
  }
}
</style>

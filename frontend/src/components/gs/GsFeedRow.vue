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
    author?: string
    meta?: string
    pinned?: boolean
  }>(),
  {
    showRank: false,
    rank: null,
    to: undefined,
    tags: undefined,
    excerpt: undefined,
    author: undefined,
    meta: undefined,
    pinned: false
  }
)

function formatRank(rank: number | string | null | undefined): string {
  if (rank == null) return ''
  const n = typeof rank === 'number' ? rank : Number(rank)
  if (!Number.isFinite(n)) return String(rank)
  return String(n).padStart(2, '0')
}
</script>

<template>
  <article class="gs-feed-row" :class="{ 'is-pinned': pinned }">
    <RouterLink v-if="to" class="gs-feed-row__inner" :to="to">
      <span
        v-if="showRank || rank != null"
        class="gs-feed-row__rank font-display"
        aria-hidden="true"
      >
        {{ formatRank(rank) }}
      </span>
      <div class="gs-feed-row__body">
        <div v-if="(tags && tags.length) || pinned" class="gs-feed-row__tags">
          <GsBadge v-if="pinned" tone="pinned">置顶</GsBadge>
          <GsTag v-for="t in tags ?? []" :key="t">{{ t }}</GsTag>
        </div>
        <h3 class="gs-feed-row__title">{{ title }}</h3>
        <p v-if="excerpt" class="gs-feed-row__excerpt">{{ excerpt }}</p>
        <div v-if="author || meta" class="gs-feed-row__byline">
          <b v-if="author">{{ author }}</b>
          <span v-if="author && meta" class="gs-feed-row__dot" aria-hidden="true" />
          <span v-if="meta">{{ meta }}</span>
        </div>
      </div>
    </RouterLink>
    <div v-else class="gs-feed-row__inner">
      <span
        v-if="showRank || rank != null"
        class="gs-feed-row__rank font-display"
        aria-hidden="true"
      >
        {{ formatRank(rank) }}
      </span>
      <div class="gs-feed-row__body">
        <div v-if="(tags && tags.length) || pinned" class="gs-feed-row__tags">
          <GsBadge v-if="pinned" tone="pinned">置顶</GsBadge>
          <GsTag v-for="t in tags ?? []" :key="t">{{ t }}</GsTag>
        </div>
        <h3 class="gs-feed-row__title">{{ title }}</h3>
        <p v-if="excerpt" class="gs-feed-row__excerpt">{{ excerpt }}</p>
        <div v-if="author || meta" class="gs-feed-row__byline">
          <b v-if="author">{{ author }}</b>
          <span v-if="author && meta" class="gs-feed-row__dot" aria-hidden="true" />
          <span v-if="meta">{{ meta }}</span>
        </div>
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
  transition: padding-left 0.2s var(--ease-out);
}

.gs-feed-row__inner:hover {
  padding-left: 8px;
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

.gs-feed-row__byline {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.8rem;
  color: var(--text-3);
  flex-wrap: wrap;
}

.gs-feed-row__byline b {
  color: var(--text-2);
  font-weight: 500;
}

.gs-feed-row__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--text-3);
}

@media (prefers-reduced-motion: reduce) {
  .gs-feed-row__inner {
    transition: none;
  }

  .gs-feed-row__inner:hover {
    padding-left: 0;
  }
}
</style>

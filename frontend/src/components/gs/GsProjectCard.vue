<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import GsTag from '@/components/gs/GsTag.vue'

const props = withDefaults(
  defineProps<{
    title: string
    to: string
    tags?: string[]
    viewCount?: number
    time?: string
    /** 一句话摘要；空则不渲染 */
    summary?: string
    /** 封面图；有则优先于字母色块 */
    coverUrl?: string | null
    /** 轻微点睛（可选）；默认按标题轮换封面色相 */
    featured?: boolean
  }>(),
  {
    tags: undefined,
    viewCount: undefined,
    time: undefined,
    summary: undefined,
    coverUrl: null,
    featured: false
  }
)

const initial = computed(() => {
  const t = props.title.trim()
  if (!t) return '?'
  return t.charAt(0).toUpperCase()
})

/** 2–3 档克制色相：按标题 hash，避免彩虹 */
const coverTone = computed(() => {
  if (props.featured) return 'accent'
  const t = props.title.trim()
  let h = 0
  for (let i = 0; i < t.length; i++) h = (h * 31 + t.charCodeAt(i)) >>> 0
  const tones = ['a', 'b', 'c'] as const
  return tones[h % tones.length]
})

const hasCoverImage = computed(() => Boolean(props.coverUrl?.trim()))
const summaryText = computed(() => props.summary?.trim() || '')
</script>

<template>
  <RouterLink
    class="gs-project-card"
    :class="[`is-tone-${coverTone}`, { 'is-featured': featured }]"
    :to="to"
  >
    <div
      class="gs-project-card__cover"
      :class="{ 'has-image': hasCoverImage }"
      aria-hidden="true"
    >
      <img
        v-if="hasCoverImage"
        class="gs-project-card__cover-img"
        :src="coverUrl!"
        alt=""
      />
      <span v-else class="gs-project-card__glyph font-display">{{ initial }}</span>
      <span v-if="viewCount != null" class="gs-project-card__views font-mono">
        浏览 {{ viewCount }}
      </span>
    </div>
    <div class="gs-project-card__body">
      <h3 class="gs-project-card__title">{{ title }}</h3>
      <p v-if="summaryText" class="gs-project-card__summary">{{ summaryText }}</p>
      <div v-if="tags && tags.length" class="gs-project-card__tags">
        <GsTag v-for="t in tags" :key="t">{{ t }}</GsTag>
      </div>
      <div v-if="time" class="gs-project-card__time font-mono">{{ time }}</div>
    </div>
  </RouterLink>
</template>

<style scoped>
.gs-project-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  text-decoration: none;
  color: inherit;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface-1);
  overflow: hidden;
  transition:
    border-color 0.2s var(--ease-out),
    transform 0.2s var(--ease-out);
}

.gs-project-card:hover {
  border-color: color-mix(in oklch, var(--accent) 45%, var(--border));
  transform: translateY(-2px);
}

.gs-project-card__cover {
  position: relative;
  height: 128px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-2);
  overflow: hidden;
}

.gs-project-card.is-tone-a .gs-project-card__cover:not(.has-image) {
  background: color-mix(in oklch, var(--surface-3) 55%, var(--surface-2));
}

.gs-project-card.is-tone-b .gs-project-card__cover:not(.has-image) {
  background: color-mix(in oklch, var(--info) 10%, var(--surface-2));
}

.gs-project-card.is-tone-c .gs-project-card__cover:not(.has-image) {
  background: color-mix(in oklch, var(--success) 8%, var(--surface-2));
}

.gs-project-card.is-tone-accent .gs-project-card__cover:not(.has-image),
.gs-project-card.is-featured .gs-project-card__cover:not(.has-image) {
  background: color-mix(in oklch, var(--accent) 12%, var(--surface-2));
}

.gs-project-card__cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gs-project-card__glyph {
  font-size: 3.25rem;
  font-weight: 700;
  line-height: 1;
  color: color-mix(in oklch, var(--text-1) 55%, transparent);
  letter-spacing: -0.03em;
  user-select: none;
}

.gs-project-card.is-tone-accent .gs-project-card__glyph,
.gs-project-card.is-featured .gs-project-card__glyph {
  color: color-mix(in oklch, var(--accent) 70%, var(--text-1));
}

.gs-project-card__views {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 0.7rem;
  color: var(--text-3);
  padding: 2px 8px;
  border-radius: 6px;
  background: color-mix(in oklch, var(--surface-1) 75%, transparent);
  border: 1px solid var(--border);
  z-index: 1;
}

.gs-project-card__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px 16px;
  flex: 1;
}

.gs-project-card__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gs-project-card__summary {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.45;
  color: var(--text-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.gs-project-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.gs-project-card__time {
  margin-top: auto;
  font-size: 0.72rem;
  color: var(--text-3);
}

@media (prefers-reduced-motion: reduce) {
  .gs-project-card {
    transition: none;
  }

  .gs-project-card:hover {
    transform: none;
  }
}
</style>

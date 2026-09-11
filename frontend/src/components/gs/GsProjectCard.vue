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
    /** 首张卡点睛：琥珀 dim 封面 */
    featured?: boolean
  }>(),
  {
    tags: undefined,
    viewCount: undefined,
    time: undefined,
    featured: false
  }
)

const initial = computed(() => {
  const t = props.title.trim()
  if (!t) return '?'
  return t.charAt(0).toUpperCase()
})
</script>

<template>
  <RouterLink class="gs-project-card" :class="{ 'is-featured': featured }" :to="to">
    <div class="gs-project-card__cover" aria-hidden="true">
      <span class="gs-project-card__glyph font-display">{{ initial }}</span>
      <span v-if="viewCount != null" class="gs-project-card__views font-mono">
        浏览 {{ viewCount }}
      </span>
    </div>
    <div class="gs-project-card__body">
      <h3 class="gs-project-card__title">{{ title }}</h3>
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

.gs-project-card.is-featured .gs-project-card__cover {
  background: color-mix(in oklch, var(--accent) 14%, var(--surface-2));
}

.gs-project-card__glyph {
  font-size: 3.25rem;
  font-weight: 700;
  line-height: 1;
  color: color-mix(in oklch, var(--text-1) 55%, transparent);
  letter-spacing: -0.03em;
  user-select: none;
}

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

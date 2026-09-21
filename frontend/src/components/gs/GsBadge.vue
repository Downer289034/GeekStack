<script setup lang="ts">
export type GsBadgeTone = 'success' | 'warning' | 'error' | 'info' | 'pinned' | 'featured'

withDefaults(
  defineProps<{
    tone?: GsBadgeTone
  }>(),
  { tone: 'info' }
)
</script>

<template>
  <span class="gs-badge" :class="`gs-badge--${tone}`">
    <slot />
  </span>
</template>

<style scoped>
.gs-badge {
  display: inline-flex;
  align-items: center;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  line-height: 1.4;
}

.gs-badge--success {
  background: color-mix(in oklch, var(--success) 15%, transparent);
  color: var(--success-on);
}

.gs-badge--warning {
  background: color-mix(in oklch, var(--warning) 15%, transparent);
  color: var(--warning-on);
}

.gs-badge--error {
  background: color-mix(in oklch, var(--error) 15%, transparent);
  color: var(--error-on);
}

.gs-badge--info {
  background: color-mix(in oklch, var(--info) 15%, transparent);
  color: var(--info-on);
}

.gs-badge--pinned {
  background: color-mix(in oklch, var(--warning) 15%, transparent);
  color: var(--warning-on);
}

.gs-badge--featured {
  background: color-mix(in oklch, var(--accent) 18%, transparent);
  color: var(--accent);
}
</style>

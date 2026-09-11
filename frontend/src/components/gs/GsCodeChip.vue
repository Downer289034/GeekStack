<script setup lang="ts">
defineProps<{
  /** 展示内容；也可用默认 slot */
  code?: string
}>()
</script>

<template>
  <code class="gs-code-chip font-mono">
    <slot>{{ code }}</slot>
  </code>
</template>

<style scoped>
.gs-code-chip {
  display: inline-flex;
  align-items: center;
  font-size: 0.72rem;
  line-height: 1.4;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  color: var(--text-2);
  font-variant-ligatures: none;
}
</style>

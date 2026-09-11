<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 可点击时渲染为 button */
    interactive?: boolean
  }>(),
  { interactive: false }
)

defineEmits<{
  click: [MouseEvent]
}>()
</script>

<template>
  <button
    v-if="interactive"
    type="button"
    class="gs-tag"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
  <span v-else class="gs-tag">
    <slot />
  </span>
</template>

<style scoped>
.gs-tag {
  display: inline-flex;
  align-items: center;
  font-size: 0.72rem;
  font-weight: 500;
  letter-spacing: 0.02em;
  padding: 3px 10px;
  border-radius: 6px;
  color: var(--text-2);
  border: 1px solid var(--border);
  background: transparent;
  font-family: inherit;
  line-height: 1.4;
  transition:
    color 0.15s var(--ease-out),
    border-color 0.15s var(--ease-out);
}

button.gs-tag {
  cursor: pointer;
}

button.gs-tag:hover,
a:hover .gs-tag {
  color: var(--accent);
  border-color: var(--accent);
}
</style>

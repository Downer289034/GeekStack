<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 可点击时渲染为 button */
    interactive?: boolean
    /** 选中态（筛选/分类） */
    selected?: boolean
  }>(),
  { interactive: false, selected: false }
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
    :class="{ 'is-selected': selected }"
    :aria-pressed="selected"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
  <span v-else class="gs-tag" :class="{ 'is-selected': selected }">
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
  min-height: 28px;
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
  min-height: 44px;
  padding: 8px 12px;
}

button.gs-tag:hover,
a:hover .gs-tag {
  color: var(--accent);
  border-color: var(--accent);
}

.gs-tag.is-selected {
  color: var(--accent-text);
  border-color: var(--accent);
  background: var(--accent);
}
</style>

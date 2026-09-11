<script setup lang="ts">
export interface GsTabItem {
  key: string
  label: string
}

const props = defineProps<{
  items: GsTabItem[]
  modelValue: string
  ariaLabel?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [string]
}>()

function select(key: string) {
  if (key === props.modelValue) return
  emit('update:modelValue', key)
}
</script>

<template>
  <div class="gs-tabs" role="tablist" :aria-label="ariaLabel">
    <button
      v-for="item in items"
      :id="`gs-tab-${item.key}`"
      :key="item.key"
      type="button"
      role="tab"
      :aria-selected="modelValue === item.key"
      :tabindex="modelValue === item.key ? 0 : -1"
      @click="select(item.key)"
    >
      {{ item.label }}
    </button>
  </div>
</template>

<style scoped>
.gs-tabs {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.gs-tabs button {
  border: none;
  background: none;
  color: var(--text-2);
  padding: 8px 2px;
  min-height: 44px;
  font-size: 0.85rem;
  font-family: inherit;
  cursor: pointer;
  position: relative;
  transition: color 0.15s var(--ease-out);
}

.gs-tabs button:hover {
  color: var(--text-1);
}

.gs-tabs button[aria-selected='true'] {
  color: var(--text-1);
  font-weight: 600;
}

.gs-tabs button[aria-selected='true']::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 6px;
  height: 2px;
  border-radius: 1px;
  background: var(--accent);
}
</style>

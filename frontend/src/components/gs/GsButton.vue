<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { RouterLink } from 'vue-router'

export type GsButtonVariant = 'primary' | 'ghost' | 'text'

const props = withDefaults(
  defineProps<{
    variant?: GsButtonVariant
    disabled?: boolean
    to?: string
    type?: 'button' | 'submit' | 'reset'
  }>(),
  {
    variant: 'ghost',
    disabled: false,
    to: undefined,
    type: 'button'
  }
)

const attrs = useAttrs()
const isLink = computed(() => Boolean(props.to) && !props.disabled)
</script>

<template>
  <RouterLink
    v-if="isLink"
    :to="to!"
    class="gs-btn"
    :class="[`gs-btn--${variant}`, { 'is-disabled': disabled }]"
    :aria-disabled="disabled || undefined"
    v-bind="attrs"
  >
    <slot />
  </RouterLink>
  <button
    v-else
    class="gs-btn"
    :class="[`gs-btn--${variant}`]"
    :type="type"
    :disabled="disabled"
    v-bind="attrs"
  >
    <slot />
  </button>
</template>

<style scoped>
.gs-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 10px;
  padding: 9px 18px;
  font-size: 0.875rem;
  font-weight: 600;
  font-family: inherit;
  line-height: 1.3;
  text-decoration: none;
  cursor: pointer;
  transition:
    color 0.15s var(--ease-out),
    background 0.15s var(--ease-out),
    border-color 0.15s var(--ease-out),
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out),
    opacity 0.15s var(--ease-out);
}

.gs-btn:disabled,
.gs-btn.is-disabled {
  opacity: 0.6;
  pointer-events: none;
  cursor: not-allowed;
}

.gs-btn--primary {
  background: var(--accent);
  color: var(--accent-text);
  border: none;
}

.gs-btn--primary:hover:not(:disabled):not(.is-disabled) {
  background: var(--accent-hover);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px color-mix(in oklch, var(--accent) 35%, transparent);
}

.gs-btn--ghost {
  background: transparent;
  color: var(--text-1);
  border: 1px solid var(--border-strong);
}

.gs-btn--ghost:hover:not(:disabled):not(.is-disabled) {
  color: var(--accent);
  border-color: var(--accent);
  transform: translateY(-1px);
}

.gs-btn--text {
  background: transparent;
  color: var(--text-2);
  border: none;
  padding: 9px 6px;
}

.gs-btn--text:hover:not(:disabled):not(.is-disabled) {
  color: var(--accent);
}
</style>

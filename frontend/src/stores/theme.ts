import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  buildNaiveOverrides,
  resolveNaiveTheme,
  type GsTheme
} from '@/theme/tokens'

const STORAGE_KEY = 'gs-theme'

function readStoredTheme(): GsTheme {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v === 'light' || v === 'dark') return v
  } catch {
    /* ignore */
  }
  return 'dark'
}

function applyDomTheme(theme: GsTheme) {
  document.documentElement.dataset.theme = theme
}

/**
 * 夜栈灯码主题：同步 html[data-theme] + localStorage。
 * 在 main.ts 于 mount 前调用 initThemeFromStorage()，避免首屏闪白。
 */
export const useThemeStore = defineStore('theme', () => {
  const theme = ref<GsTheme>(readStoredTheme())

  const naiveTheme = computed(() => resolveNaiveTheme(theme.value))
  const naiveOverrides = computed(() => buildNaiveOverrides(theme.value))

  function setTheme(next: GsTheme) {
    theme.value = next
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  watch(
    theme,
    (v) => {
      applyDomTheme(v)
      try {
        localStorage.setItem(STORAGE_KEY, v)
      } catch {
        /* ignore */
      }
    },
    { immediate: true }
  )

  return { theme, naiveTheme, naiveOverrides, setTheme, toggleTheme }
})

/** 非 Vue 上下文：尽早写入 data-theme */
export function initThemeFromStorage(): GsTheme {
  const theme = readStoredTheme()
  applyDomTheme(theme)
  return theme
}

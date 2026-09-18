<script setup lang="ts">
import { NConfigProvider } from 'naive-ui'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import GsParticles from '@/components/gs/GsParticles.vue'

const themeStore = useThemeStore()
const { naiveTheme, naiveOverrides } = storeToRefs(themeStore)
</script>

<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="naiveOverrides">
    <GsParticles />
    <router-view />
  </n-config-provider>
</template>

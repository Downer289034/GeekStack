<script setup lang="ts">
import { NConfigProvider, NMessageProvider } from 'naive-ui'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'
import AiChatWidget from '@/components/gs/AiChatWidget.vue'
import GsParticles from '@/components/gs/GsParticles.vue'

const themeStore = useThemeStore()
const { naiveTheme, naiveOverrides } = storeToRefs(themeStore)
</script>

<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="naiveOverrides">
    <n-message-provider>
      <GsParticles />
      <router-view />
      <AiChatWidget />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { NConfigProvider } from 'naive-ui'
import { naiveThemeOverrides } from '@/theme/tokens'
</script>

<template>
  <n-config-provider :theme-overrides="naiveThemeOverrides">
    <router-view />
  </n-config-provider>
</template>

<script setup lang="ts">
import { NButton, NCard, NSpace, NConfigProvider } from 'naive-ui'
import { naiveThemeOverrides } from '@/theme/tokens'
</script>

<template>
  <n-config-provider :theme-overrides="naiveThemeOverrides">
    <main class="scaffold">
      <n-card title="Java & AI 交流社区" class="card">
        <n-space vertical>
          <p>前端骨架已就绪（T4）：鸿蒙设计风主题 token、请求封装、Pinia 登录态。</p>
          <n-button type="primary">主题按钮</n-button>
        </n-space>
      </n-card>
    </main>
  </n-config-provider>
</template>

<style scoped>
.scaffold {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {
  width: 480px;
}
</style>

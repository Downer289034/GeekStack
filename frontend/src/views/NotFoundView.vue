<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { NButton, NEmpty, NIcon } from 'naive-ui'
import { HomeOutline, WarningOutline } from '@vicons/ionicons5'
</script>

<template>
  <div class="not-found">
    <n-empty size="huge" description="404 · 页面不存在">
      <template #icon>
        <n-icon :component="WarningOutline" :size="48" />
      </template>
      <template #extra>
        <RouterLink to="/">
          <n-button type="primary">
            <template #icon>
              <n-icon :component="HomeOutline" />
            </template>
            回到首页
          </n-button>
        </RouterLink>
      </template>
    </n-empty>
  </div>
</template>

<style scoped>
.not-found {
  max-width: 480px;
  margin: 96px auto;
}
</style>

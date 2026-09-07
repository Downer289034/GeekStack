<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { NButton, NEmpty } from 'naive-ui'
</script>

<template>
  <div class="not-found">
    <n-empty size="huge" description="404 · 页面不存在">
      <template #extra>
        <RouterLink to="/">
          <n-button type="primary">回到首页</n-button>
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

<script setup lang="ts">
import { NEmpty, NIcon } from 'naive-ui'
import { HomeOutline, WarningOutline } from '@vicons/ionicons5'
import GsButton from '@/components/gs/GsButton.vue'
</script>

<template>
  <div class="not-found">
    <p class="not-found__code font-display" aria-hidden="true">404</p>
    <n-empty size="huge" description="页面不存在">
      <template #icon>
        <n-icon :component="WarningOutline" :size="48" />
      </template>
      <template #extra>
        <GsButton variant="primary" to="/">
          <n-icon :component="HomeOutline" :size="16" />
          回到首页
        </GsButton>
      </template>
    </n-empty>
  </div>
</template>

<style scoped>
.not-found {
  max-width: 480px;
  margin: 96px auto;
  text-align: center;
}

.not-found__code {
  margin: 0 0 8px;
  font-size: 4rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: color-mix(in oklch, var(--accent) 55%, var(--text-3));
  line-height: 1;
}
</style>

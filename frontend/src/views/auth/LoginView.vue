<script setup lang="ts">
import { NCard, NTabPane, NTabs } from 'naive-ui'
</script>

<template>
  <div class="login-wrap">
    <n-card class="login-card" title="JavaAI 社区">
      <n-tabs type="segment" animated>
        <n-tab-pane name="login" tab="登录">
          <p>骨架占位页（T5）：登录表单将在 Task 8 实现。</p>
        </n-tab-pane>
        <n-tab-pane name="register" tab="注册">
          <p>骨架占位页（T5）：注册表单将在 Task 8 实现。</p>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 420px;
}
</style>

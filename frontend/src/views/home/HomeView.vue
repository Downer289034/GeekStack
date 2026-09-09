<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { NButton, NIcon } from 'naive-ui'
import { AddOutline, ChatbubblesOutline } from '@vicons/ionicons5'
</script>

<template>
  <section class="home-page" aria-label="首页">
    <div class="hero">
      <div class="hero__deco" aria-hidden="true" />
      <div class="hero__content">
        <p class="hero__brand">极客栈 GeekStack</p>
        <h1 class="hero__slogan">代码江湖的落脚点</h1>
        <p class="hero__sub">极客落脚，技术常新——分享项目、讨论技术，找到同路人</p>
        <div class="hero__actions">
          <RouterLink to="/shares/new">
            <n-button class="hero__btn-primary" type="default" size="medium">
              <template #icon>
                <n-icon :component="AddOutline" />
              </template>
              发布项目
            </n-button>
          </RouterLink>
          <RouterLink to="/forum">
            <n-button class="hero__btn-ghost" ghost size="medium">
              <template #icon>
                <n-icon :component="ChatbubblesOutline" />
              </template>
              逛论坛
            </n-button>
          </RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero {
  position: relative;
  overflow: hidden;
  min-height: 180px;
  padding: 28px 32px;
  border-radius: var(--radius);
  background: linear-gradient(
    135deg,
    var(--color-primary-pressed) 0%,
    var(--color-primary) 45%,
    var(--color-primary-soft) 100%
  );
  box-shadow: var(--shadow-sm);
  color: var(--color-bg-container);
}

.hero__deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(
      circle at 88% 28%,
      color-mix(in srgb, var(--color-bg-container) 15%, transparent) 0%,
      transparent 42%
    ),
    radial-gradient(
      circle at 72% 78%,
      color-mix(in srgb, var(--color-bg-container) 10%, transparent) 0%,
      transparent 36%
    ),
    repeating-linear-gradient(
      -12deg,
      transparent 0,
      transparent 14px,
      color-mix(in srgb, var(--color-bg-container) 6%, transparent) 14px,
      color-mix(in srgb, var(--color-bg-container) 6%, transparent) 15px
    );
  opacity: 0.9;
}

.hero__content {
  position: relative;
  z-index: 1;
  max-width: 640px;
}

.hero__brand {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.04em;
  opacity: 0.92;
}

.hero__slogan {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--color-bg-container);
}

.hero__sub {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.55;
  opacity: 0.92;
  max-width: 36em;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero__btn-primary {
  --n-color: var(--color-bg-container) !important;
  --n-color-hover: color-mix(in srgb, var(--color-primary) 6%, var(--color-bg-container)) !important;
  --n-color-pressed: color-mix(in srgb, var(--color-primary) 12%, var(--color-bg-container)) !important;
  --n-color-focus: var(--color-bg-container) !important;
  --n-text-color: var(--color-primary) !important;
  --n-text-color-hover: var(--color-primary) !important;
  --n-text-color-pressed: var(--color-primary-pressed) !important;
  --n-border: 1px solid var(--color-bg-container) !important;
  --n-border-hover: 1px solid var(--color-bg-container) !important;
  --n-border-pressed: 1px solid var(--color-bg-container) !important;
  --n-border-focus: 1px solid var(--color-bg-container) !important;
  font-weight: 600;
}

.hero__btn-ghost {
  --n-text-color: var(--color-bg-container) !important;
  --n-text-color-hover: var(--color-bg-container) !important;
  --n-text-color-pressed: var(--color-bg-container) !important;
  --n-border: 1px solid color-mix(in srgb, var(--color-bg-container) 85%, transparent) !important;
  --n-border-hover: 1px solid var(--color-bg-container) !important;
  --n-border-pressed: 1px solid var(--color-bg-container) !important;
  --n-border-focus: 1px solid var(--color-bg-container) !important;
  --n-color: transparent !important;
  --n-color-hover: color-mix(in srgb, var(--color-bg-container) 12%, transparent) !important;
  --n-color-pressed: color-mix(in srgb, var(--color-bg-container) 18%, transparent) !important;
  --n-color-focus: transparent !important;
  font-weight: 500;
}

/* 平板 ≤768 */
@media (max-width: 768px) {
  .hero {
    padding: 24px 20px;
    min-height: 168px;
  }

  .hero__slogan {
    font-size: 24px;
  }
}

/* 手机 ≤375 口径：小屏收纳 */
@media (max-width: 480px) {
  .hero {
    padding: 20px 16px;
    min-height: 160px;
  }

  .hero__slogan {
    font-size: 22px;
  }

  .hero__sub {
    font-size: 13px;
    margin-bottom: 16px;
  }

  .hero__actions {
    width: 100%;
  }

  .hero__actions :deep(a) {
    flex: 1 1 auto;
  }

  .hero__actions :deep(.n-button) {
    width: 100%;
  }
}
</style>

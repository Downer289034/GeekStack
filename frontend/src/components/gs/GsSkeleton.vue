<script setup lang="ts">
withDefaults(
  defineProps<{
    /** block=色块；row=信息流行；card=项目卡；detail=详情；home=首页三区 */
    variant?: 'block' | 'row' | 'card' | 'detail' | 'home'
    /** row/card 重复份数 */
    count?: number
  }>(),
  {
    variant: 'block',
    count: 3
  }
)
</script>

<template>
  <div
    class="gs-skel"
    :class="`gs-skel--${variant}`"
    role="status"
    aria-live="polite"
    aria-busy="true"
  >
    <span class="sr-only">加载中</span>

    <template v-if="variant === 'block'">
      <span class="gs-skeleton-block gs-skel__block" />
    </template>

    <template v-else-if="variant === 'row'">
      <div v-for="i in count" :key="i" class="gs-skel__row">
        <span class="gs-skeleton-block gs-skel__rank" />
        <div class="gs-skel__row-body">
          <span class="gs-skeleton-block gs-skel__line gs-skel__line--sm" />
          <span class="gs-skeleton-block gs-skel__line gs-skel__line--lg" />
          <span class="gs-skeleton-block gs-skel__line gs-skel__line--md" />
        </div>
      </div>
    </template>

    <template v-else-if="variant === 'card'">
      <div class="gs-skel__cards">
        <div v-for="i in count" :key="i" class="gs-skel__card">
          <span class="gs-skeleton-block gs-skel__cover" />
          <div class="gs-skel__card-body">
            <span class="gs-skeleton-block gs-skel__line gs-skel__line--lg" />
            <span class="gs-skeleton-block gs-skel__line gs-skel__line--md" />
            <span class="gs-skeleton-block gs-skel__line gs-skel__line--sm" />
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="variant === 'detail'">
      <span class="gs-skeleton-block gs-skel__line gs-skel__line--title" />
      <span class="gs-skeleton-block gs-skel__line gs-skel__line--md" />
      <span class="gs-skeleton-block gs-skel__line gs-skel__line--md" />
      <span class="gs-skeleton-block gs-skel__para" />
      <span class="gs-skeleton-block gs-skel__para" />
      <span class="gs-skeleton-block gs-skel__para gs-skel__para--short" />
    </template>

    <template v-else-if="variant === 'home'">
      <div class="gs-skel__home">
        <div class="gs-skel__home-feed">
          <span class="gs-skeleton-block gs-skel__featured" />
          <div v-for="i in 4" :key="i" class="gs-skel__row">
            <span class="gs-skeleton-block gs-skel__rank" />
            <div class="gs-skel__row-body">
              <span class="gs-skeleton-block gs-skel__line gs-skel__line--lg" />
              <span class="gs-skeleton-block gs-skel__line gs-skel__line--md" />
            </div>
          </div>
        </div>
        <div class="gs-skel__home-side">
          <span class="gs-skeleton-block gs-skel__side-block" />
          <span class="gs-skeleton-block gs-skel__side-block" />
          <span class="gs-skeleton-block gs-skel__side-block gs-skel__side-block--sm" />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.gs-skel__block {
  width: 100%;
  height: 48px;
}

.gs-skel__row {
  display: flex;
  gap: 18px;
  padding: 20px 0;
  border-bottom: 1px solid var(--border);
}

.gs-skel__rank {
  width: 28px;
  height: 22px;
  flex-shrink: 0;
  margin-top: 2px;
}

.gs-skel__row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.gs-skel__line {
  height: 12px;
}

.gs-skel__line--sm {
  width: 28%;
}

.gs-skel__line--md {
  width: 55%;
}

.gs-skel__line--lg {
  width: 78%;
  height: 16px;
}

.gs-skel__line--title {
  width: 62%;
  height: 28px;
  margin-bottom: 16px;
}

.gs-skel__cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .gs-skel__cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .gs-skel__cards {
    grid-template-columns: 1fr;
  }
}

.gs-skel__card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface-1);
}

.gs-skel__cover {
  height: 128px;
  border-radius: 0;
}

.gs-skel__card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px 16px;
}

.gs-skel--detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 0;
}

.gs-skel__para {
  width: 100%;
  max-width: 62ch;
  height: 14px;
}

.gs-skel__para--short {
  width: 42%;
}

.gs-skel__home {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 48px;
  align-items: start;
}

@media (max-width: 960px) {
  .gs-skel__home {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

.gs-skel__featured {
  width: 100%;
  height: 120px;
  margin-bottom: 8px;
}

.gs-skel__home-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.gs-skel__side-block {
  height: 160px;
}

.gs-skel__side-block--sm {
  height: 88px;
}
</style>

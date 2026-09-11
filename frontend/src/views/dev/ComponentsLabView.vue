<script setup lang="ts">
import { ref } from 'vue'
import GsBadge from '@/components/gs/GsBadge.vue'
import GsButton from '@/components/gs/GsButton.vue'
import GsCodeChip from '@/components/gs/GsCodeChip.vue'
import GsFeedFeatured from '@/components/gs/GsFeedFeatured.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsProjectCard from '@/components/gs/GsProjectCard.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsTag from '@/components/gs/GsTag.vue'
import GsUnderlineTabs from '@/components/gs/GsUnderlineTabs.vue'

const tab = ref('latest')
const tabs = [
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '热门' },
  { key: 'ai', label: 'AI' },
  { key: 'arch', label: '架构' }
]
</script>

<template>
  <div class="lab">
    <header class="lab-head">
      <h1 class="font-display">Gs 组件实验室</h1>
      <p class="lab-sub">对照 geekstack-ui-design.html §5–§6 · 夜栈灯码 v1.1</p>
    </header>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Buttons</h2>
      <div class="lab-row">
        <GsButton variant="primary">写文章</GsButton>
        <GsButton variant="ghost">次要操作</GsButton>
        <GsButton variant="text">文字按钮</GsButton>
        <GsButton variant="primary" disabled>禁用 Primary</GsButton>
        <GsButton variant="ghost" disabled>禁用 Ghost</GsButton>
      </div>
    </section>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Underline Tabs</h2>
      <GsUnderlineTabs v-model="tab" :items="tabs" aria-label="示例过滤" />
    </section>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Tags / Badges / Code chip</h2>
      <div class="lab-row">
        <GsTag>中性标签</GsTag>
        <GsTag interactive>可点标签</GsTag>
        <GsBadge tone="success">已采纳</GsBadge>
        <GsBadge tone="warning">审核中</GsBadge>
        <GsBadge tone="error">已驳回</GsBadge>
        <GsBadge tone="info">置顶</GsBadge>
        <GsBadge tone="pinned">置顶</GsBadge>
        <GsCodeChip>spring-boot-starter</GsCodeChip>
        <GsCodeChip>Vue3</GsCodeChip>
      </div>
    </section>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Feed · Featured</h2>
      <GsFeedFeatured
        title="把 LangGraph 塞进 Spring Boot：一次内容审核 Agent 的编排实战"
        :tags="['AI 落地', '深度']"
        excerpt="当 checkpointer 遇上 PG、当 Celery Beat 遇上 RAG 召回——本文复盘一条内容审核流水线从原型到生产的完整链路。"
        author="阿栈"
        meta="2 小时前 · 1.2k 阅读"
        to="/forum"
      />
    </section>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Skeleton</h2>
      <GsSkeleton variant="row" :count="2" />
      <GsSkeleton variant="card" :count="2" />
    </section>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Reveal (scroll into view)</h2>
      <div v-reveal class="lab-reveal-demo">
        <p>这条块带 v-reveal，滚入视口后 translateY(10px)→0</p>
      </div>
    </section>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Project Card</h2>
      <div class="lab-grid">
        <GsProjectCard
          title="GeekStack 夜栈灯码"
          to="/shares"
          :tags="['Vue3', 'Design Token']"
          :view-count="128"
          time="2026-04-08 12:00:00"
          featured
        />
        <GsProjectCard
          title="内容审核 Agent"
          to="/shares"
          :tags="['LangGraph', 'Spring']"
          :view-count="86"
          time="2026-04-07 09:30:00"
        />
      </div>
    </section>

    <section class="lab-block">
      <h2 class="lab-label font-mono">Feed · Rows</h2>
      <GsFeedRow
        :rank="1"
        show-rank
        title="Vite 插件里做 Vue Macro 的正确姿势"
        excerpt="从 unplugin 生命周期讲起，避免编译期副作用泄漏到 SSR。"
        :tags="['工程化']"
        meta="86 阅读 · 12 赞"
        to="/forum"
      />
      <GsFeedRow
        :rank="2"
        show-rank
        title="MyBatis-Plus 逻辑删除与唯一索引冲突怎么解"
        excerpt="软删字段进唯一键的几种折中，以及线上踩坑。"
        :tags="['数据库']"
        pinned
        meta="210 阅读"
        to="/forum"
      />
      <GsFeedRow
        :rank="3"
        show-rank
        title="Redis Stream 做站内通知的取舍"
        meta="热度 128"
        to="/forum"
      />
    </section>
  </div>
</template>

<style scoped>
.lab {
  display: flex;
  flex-direction: column;
  gap: 40px;
  padding-bottom: 48px;
}

.lab-head h1 {
  margin: 0;
  font-size: 1.6rem;
  letter-spacing: -0.01em;
}

.lab-sub {
  margin: 6px 0 0;
  color: var(--text-2);
  font-size: 0.925rem;
}

.lab-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.lab-label {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-3);
}

.lab-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.lab-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.lab-reveal-demo {
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text-2);
}

@media (max-width: 768px) {
  .lab-grid {
    grid-template-columns: 1fr;
  }
}
</style>

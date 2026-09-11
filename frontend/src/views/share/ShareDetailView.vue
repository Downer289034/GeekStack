<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NEmpty,
  NIcon,
  NPageHeader,
  NPopconfirm,
  NSpace,
  useMessage
} from 'naive-ui'
import { AlertCircleOutline } from '@vicons/ionicons5'
import { fetchShare, hideShare, type ShareResponse } from '@/api/share'
import GsBadge from '@/components/gs/GsBadge.vue'
import GsButton from '@/components/gs/GsButton.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsTag from '@/components/gs/GsTag.vue'
import { renderMarkdown } from '@/utils/markdown'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = useMessage()

const share = ref<ShareResponse | null>(null)
const loading = ref(true)
const notFound = ref(false)
const hiding = ref(false)

const renderedContent = computed(() =>
  share.value ? renderMarkdown(share.value.contentMd) : ''
)

const isOwner = computed(
  () => auth.isLoggedIn && auth.user != null && share.value != null && auth.user.id === share.value.userId
)

async function load() {
  loading.value = true
  try {
    share.value = await fetchShare(route.params.id as string)
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function onHide() {
  if (!share.value) return
  hiding.value = true
  try {
    await hideShare(share.value.id)
    message.success('已下架')
    void router.push('/shares')
  } finally {
    hiding.value = false
  }
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div v-if="notFound" class="detail-state">
    <n-empty size="huge" description="项目分享不存在，可能已被删除或下架">
      <template #icon>
        <n-icon :component="AlertCircleOutline" :size="48" />
      </template>
      <template #extra>
        <GsButton variant="primary" to="/shares">返回项目广场</GsButton>
      </template>
    </n-empty>
  </div>

  <div v-else class="detail">
    <n-page-header :title="share?.title ?? (loading ? '…' : '加载中…')" @back="router.back()" />

    <GsSkeleton v-if="loading" variant="detail" />

    <article v-else-if="share" v-reveal class="detail-card">
      <div class="detail-card__toolbar">
        <p class="detail-summary">{{ share.summary }}</p>
        <n-space v-if="isOwner" :wrap="true">
          <GsButton variant="ghost" :to="`/shares/edit/${share.id}`">编辑</GsButton>
          <n-popconfirm @positive-click="onHide">
            <template #trigger>
              <GsButton variant="text" :disabled="hiding">下架</GsButton>
            </template>
            下架后其他人将无法看到该项目，确定吗？
          </n-popconfirm>
        </n-space>
      </div>

      <div class="detail-tags">
        <GsTag>项目</GsTag>
        <GsTag v-for="t in share.tags ?? []" :key="t">{{ t }}</GsTag>
      </div>

      <dl class="detail-meta font-mono">
        <div>
          <dt>发布时间</dt>
          <dd>{{ formatTime(share.createdAt) }}</dd>
        </div>
        <div>
          <dt>浏览量</dt>
          <dd>{{ share.viewCount }}</dd>
        </div>
        <div>
          <dt>状态</dt>
          <dd>
            <GsBadge :tone="share.status === 'PUBLISHED' ? 'success' : 'error'">
              {{ share.status === 'PUBLISHED' ? '已发布' : '已下架' }}
            </GsBadge>
          </dd>
        </div>
      </dl>

      <div class="detail-links">
        <a
          v-if="share.repoUrl"
          :href="share.repoUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          <GsButton variant="primary">仓库地址</GsButton>
        </a>
        <a
          v-if="share.demoUrl"
          :href="share.demoUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          <GsButton variant="ghost">在线 Demo</GsButton>
        </a>
      </div>

      <!-- eslint-disable-next-line vue/no-v-html -- 内容经 utils/markdown.ts 的 DOMPurify 白名单消毒，全站唯一渲染出口 -->
      <div class="detail-content" v-html="renderedContent"></div>
    </article>
  </div>
</template>

<style scoped>
.detail-card {
  margin-top: 16px;
}

.detail-card__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.detail-summary {
  flex: 1;
  min-width: 0;
  font-size: 0.95rem;
  color: var(--text-2);
  margin: 0;
  max-width: 62ch;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 16px;
  padding: 12px 0;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}

.detail-meta dt {
  margin: 0 0 4px;
  font-size: 0.7rem;
  color: var(--text-3);
}

.detail-meta dd {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-1);
}

@media (max-width: 768px) {
  .detail-meta {
    grid-template-columns: 1fr;
  }
}

.detail-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.detail-links a {
  text-decoration: none;
}

.detail-content {
  line-height: 1.75;
  word-break: break-word;
  color: var(--text-1);
  max-width: 62ch;
}

.detail-state {
  max-width: 480px;
  margin: 64px auto;
}
</style>

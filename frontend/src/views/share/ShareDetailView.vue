<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NIcon,
  NPageHeader,
  NPopconfirm,
  NSpace,
  NTag,
  useMessage
} from 'naive-ui'
import { AlertCircleOutline, CubeOutline } from '@vicons/ionicons5'
import { fetchShare, hideShare, type ShareResponse } from '@/api/share'
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
        <RouterLink to="/shares">
          <n-button type="primary">返回项目广场</n-button>
        </RouterLink>
      </template>
    </n-empty>
  </div>

  <div v-else class="detail">
    <n-page-header :title="share?.title ?? '加载中…'" @back="router.back()" />

    <n-card v-if="share" title="项目信息" class="gs-card-detail detail-card" :bordered="false">
      <template #header-extra>
        <n-space v-if="isOwner">
          <RouterLink :to="`/shares/edit/${share.id}`">
            <n-button size="small">编辑</n-button>
          </RouterLink>
          <n-popconfirm @positive-click="onHide">
            <template #trigger>
              <n-button size="small" type="warning" :loading="hiding">下架</n-button>
            </template>
            下架后其他人将无法看到该项目，确定吗？
          </n-popconfirm>
        </n-space>
      </template>

      <p class="detail-summary">{{ share.summary }}</p>
      <n-space size="small" class="detail-tags">
        <n-tag class="gs-tag-share" size="small" :bordered="false">
          <template #icon>
            <n-icon :component="CubeOutline" />
          </template>
          项目
        </n-tag>
        <n-tag
          v-for="t in share.tags ?? []"
          :key="t"
          class="gs-tag-share"
          size="small"
          :bordered="false"
        >
          {{ t }}
        </n-tag>
      </n-space>

      <n-descriptions size="small" :column="3" class="detail-meta" label-placement="left">
        <n-descriptions-item label="发布时间">{{ formatTime(share.createdAt) }}</n-descriptions-item>
        <n-descriptions-item label="浏览量">{{ share.viewCount }}</n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag
            size="small"
            :type="share.status === 'PUBLISHED' ? 'success' : 'error'"
            :bordered="false"
          >
            {{ share.status === 'PUBLISHED' ? '已发布' : '已下架' }}
          </n-tag>
        </n-descriptions-item>
      </n-descriptions>

      <n-space class="detail-links">
        <a v-if="share.repoUrl" :href="share.repoUrl" target="_blank" rel="noopener noreferrer">
          <n-button size="small" type="primary">仓库地址</n-button>
        </a>
        <a v-if="share.demoUrl" :href="share.demoUrl" target="_blank" rel="noopener noreferrer">
          <n-button size="small">在线 Demo</n-button>
        </a>
      </n-space>

      <!-- eslint-disable-next-line vue/no-v-html -- 内容经 utils/markdown.ts 的 DOMPurify 白名单消毒，全站唯一渲染出口 -->
      <div class="detail-content" v-html="renderedContent"></div>
    </n-card>
  </div>
</template>

<style scoped>
.detail-card {
  margin-top: 16px;
}

.detail-summary {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0 0 12px;
}

.detail-tags {
  margin-bottom: 12px;
}

.detail-meta {
  margin-bottom: 12px;
}

.detail-links {
  margin-bottom: 16px;
}

.detail-content {
  line-height: 1.75;
  word-break: break-word;
}

.detail-state {
  max-width: 480px;
  margin: 64px auto;
}
</style>

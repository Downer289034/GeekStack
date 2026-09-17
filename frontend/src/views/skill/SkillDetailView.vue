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
import { AlertCircleOutline, FlashOutline } from '@vicons/ionicons5'
import { fetchSkill, hideSkill, type SkillResponse } from '@/api/skill'
import GsBadge from '@/components/gs/GsBadge.vue'
import GsButton from '@/components/gs/GsButton.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsTag from '@/components/gs/GsTag.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = useMessage()

const skill = ref<SkillResponse | null>(null)
const loading = ref(true)
const notFound = ref(false)
const hiding = ref(false)

const isOwner = computed(
  () =>
    auth.isLoggedIn &&
    auth.user != null &&
    skill.value != null &&
    auth.user.id === skill.value.userId
)

async function load() {
  loading.value = true
  try {
    skill.value = await fetchSkill(route.params.id as string)
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function onHide() {
  if (!skill.value) return
  hiding.value = true
  try {
    await hideSkill(skill.value.id)
    message.success('已下架')
    void router.push('/skills')
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
    <n-empty size="huge" description="Skill 不存在，可能已被删除或下架">
      <template #icon>
        <n-icon :component="AlertCircleOutline" :size="48" />
      </template>
      <template #extra>
        <GsButton variant="primary" to="/skills">返回 Skill 广场</GsButton>
      </template>
    </n-empty>
  </div>

  <div v-else class="detail">
    <n-page-header :title="skill?.title ?? (loading ? '…' : '加载中…')" @back="router.back()" />

    <GsSkeleton v-if="loading" variant="detail" />

    <article v-else-if="skill" v-reveal class="detail-card">
      <div class="detail-card__toolbar">
        <p v-if="skill.summary" class="detail-summary">{{ skill.summary }}</p>
        <n-space v-if="isOwner" :wrap="true">
          <GsButton variant="ghost" :to="`/skills/edit/${skill.id}`">编辑</GsButton>
          <n-popconfirm @positive-click="onHide">
            <template #trigger>
              <GsButton variant="text" :disabled="hiding">下架</GsButton>
            </template>
            下架后其他人将无法看到该 Skill，确定吗？
          </n-popconfirm>
        </n-space>
      </div>

      <div class="detail-tags">
        <GsTag>
          <n-icon :component="FlashOutline" :size="12" />
          Skill
        </GsTag>
        <GsTag v-for="t in skill.tags ?? []" :key="t">{{ t }}</GsTag>
      </div>

      <section v-if="skill.modelNotes" class="model-notes" aria-label="模型适配说明">
        <h3 class="model-notes__title font-mono">模型适配</h3>
        <p class="model-notes__body">{{ skill.modelNotes }}</p>
      </section>

      <dl class="detail-meta font-mono">
        <div>
          <dt>发布时间</dt>
          <dd>{{ formatTime(skill.createdAt) }}</dd>
        </div>
        <div>
          <dt>浏览量</dt>
          <dd>{{ skill.viewCount }}</dd>
        </div>
        <div>
          <dt>状态</dt>
          <dd>
            <GsBadge :tone="skill.status === 'PUBLISHED' ? 'success' : 'error'">
              {{ skill.status === 'PUBLISHED' ? '已发布' : '已下架' }}
            </GsBadge>
          </dd>
        </div>
      </dl>

      <h3 class="prompt-heading font-display">提示词</h3>
      <pre class="prompt-block font-mono" tabindex="0">{{ skill.promptMd }}</pre>
    </article>
  </div>
</template>

<style scoped>
.detail-state {
  max-width: 480px;
  margin: 64px auto;
}

.detail-card {
  margin-top: 16px;
  padding-bottom: 24px;
}

.detail-card__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-summary {
  margin: 0;
  flex: 1;
  color: var(--text-2);
  line-height: 1.6;
  max-width: 62ch;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.model-notes {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-small);
  background: var(--surface-2);
}

.model-notes__title {
  margin: 0 0 6px;
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-3);
}

.model-notes__body {
  margin: 0;
  color: var(--text-1);
  line-height: 1.6;
  white-space: pre-wrap;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 28px;
  margin: 0 0 20px;
  padding: 0;
}

.detail-meta div {
  margin: 0;
}

.detail-meta dt {
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-3);
}

.detail-meta dd {
  margin: 4px 0 0;
  color: var(--text-1);
}

.prompt-heading {
  margin: 0 0 10px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-1);
}

.prompt-block {
  margin: 0;
  padding: 16px 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-small);
  background: var(--surface-2);
  color: var(--text-1);
  font-size: 0.875rem;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
  max-width: 100%;
}

.prompt-block:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  NAvatar,
  NEmpty,
  NIcon,
  NInput,
  NPageHeader,
  NPopconfirm,
  NSpace,
  NText,
  useMessage
} from 'naive-ui'
import { AlertCircleOutline, ChatbubbleOutline, HeartOutline } from '@vicons/ionicons5'
import {
  createComment,
  deleteComment,
  deletePost,
  fetchComments,
  fetchPost,
  type CommentNode,
  type PostDetail
} from '@/api/forum'
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

const post = ref<PostDetail | null>(null)
const comments = ref<CommentNode[]>([])
const notFound = ref(false)
const loading = ref(true)
const replyText = ref('')
const nestedReplyTo = ref<CommentNode | null>(null)
const submitting = ref(false)

const renderedContent = computed(() => (post.value ? renderMarkdown(post.value.contentMd) : ''))
const isOwner = computed(
  () => auth.isLoggedIn && auth.user != null && post.value != null && auth.user.id === post.value.userId
)

function canModerate(comment: CommentNode): boolean {
  return auth.isLoggedIn && auth.user != null && auth.user.id === comment.userId
}

async function loadAll() {
  loading.value = true
  const id = route.params.id as string
  try {
    post.value = await fetchPost(id)
    comments.value = await fetchComments('posts', id)
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function submitReply() {
  if (!post.value || !replyText.value.trim()) {
    return
  }
  submitting.value = true
  try {
    await createComment(
      'posts',
      post.value.id,
      replyText.value.trim(),
      nestedReplyTo.value?.id
    )
    replyText.value = ''
    nestedReplyTo.value = null
    comments.value = await fetchComments('posts', post.value.id)
    if (post.value) {
      post.value.replyCount += 1
      post.value.lastReplyAt = new Date().toISOString()
    }
    message.success('回帖成功')
  } finally {
    submitting.value = false
  }
}

function startNestedReply(comment: CommentNode) {
  nestedReplyTo.value = comment
}

function cancelNestedReply() {
  nestedReplyTo.value = null
}

async function onDeleteComment(comment: CommentNode) {
  if (!post.value) return
  await deleteComment('posts', comment.id)
  comments.value = await fetchComments('posts', post.value.id)
  if (post.value.replyCount > 0) {
    post.value.replyCount -= 1
  }
  message.success('已删除')
}

async function onDeletePost() {
  if (!post.value) return
  await deletePost(post.value.id)
  message.success('帖子已删除')
  void router.push('/forum')
}

function formatTime(iso: string | null): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  void loadAll()
})
</script>

<template>
  <div v-if="notFound" class="post-state">
    <n-empty size="huge" description="帖子不存在，可能已被作者删除">
      <template #icon>
        <n-icon :component="AlertCircleOutline" :size="48" />
      </template>
      <template #extra>
        <GsButton variant="primary" to="/forum">返回论坛</GsButton>
      </template>
    </n-empty>
  </div>

  <div v-else class="post-detail">
    <n-page-header :title="post?.title ?? (loading ? '…' : '加载中…')" @back="router.back()" />

    <GsSkeleton v-if="loading" variant="detail" />

    <article v-else-if="post" v-reveal class="post-card">
      <div class="post-card__toolbar">
        <n-space align="center" :wrap="true" class="post-meta">
          <GsBadge v-if="post.isPinned === 1" tone="pinned">置顶</GsBadge>
          <GsBadge v-if="post.isFeatured === 1" tone="featured">精华</GsBadge>
          <RouterLink
            v-for="tag in post.tags ?? []"
            :key="tag"
            class="post-tag-link"
            :to="{ path: '/forum', query: { tag } }"
          >
            <GsTag interactive>{{ tag }}</GsTag>
          </RouterLink>
          <GsTag>浏览 {{ post.viewCount }}</GsTag>
          <GsTag>回帖 {{ post.replyCount }}</GsTag>
          <span class="post-time font-mono">{{ formatTime(post.createdAt) }}</span>
        </n-space>
        <n-popconfirm v-if="isOwner" @positive-click="onDeletePost">
          <template #trigger>
            <GsButton variant="text">删除</GsButton>
          </template>
          删除后所有人不可见，确定吗？
        </n-popconfirm>
      </div>

      <!-- eslint-disable-next-line vue/no-v-html -- 经 utils/markdown.ts DOMPurify 消毒（全站唯一出口） -->
      <div class="post-content" v-html="renderedContent"></div>
    </article>

    <section v-if="!loading && post" class="reply-card">
      <h3 class="reply-title font-display">
        <n-icon :component="ChatbubbleOutline" :size="18" />
        回帖（{{ post?.replyCount ?? 0 }}）
      </h3>

      <n-empty v-if="comments.length === 0" description="还没有回帖，来抢沙发" size="small">
        <template #icon>
          <n-icon :component="HeartOutline" :size="40" />
        </template>
      </n-empty>

      <div v-for="comment in comments" :key="comment.id" class="comment-block">
        <div class="comment">
          <n-avatar round :size="32" class="comment-avatar">{{ comment.userId }}</n-avatar>
          <div class="comment-body">
            <div class="comment-head">
              <span class="comment-user">用户 #{{ comment.userId }}</span>
              <span class="comment-time font-mono">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <!-- eslint-disable-next-line vue/no-v-html -- 经 utils/markdown.ts DOMPurify 消毒 -->
            <div class="comment-content" v-html="renderMarkdown(comment.contentMd)"></div>
            <div class="comment-actions">
              <GsButton variant="text" @click="startNestedReply(comment)">回复</GsButton>
              <n-popconfirm v-if="canModerate(comment)" @positive-click="onDeleteComment(comment)">
                <template #trigger>
                  <GsButton variant="text">删除</GsButton>
                </template>
                删除这条回帖？
              </n-popconfirm>
            </div>
          </div>
        </div>

        <div v-if="comment.children.length > 0" class="nested-list">
          <div v-for="child in comment.children" :key="child.id" class="comment nested">
            <n-avatar round :size="28" class="comment-avatar">{{ child.userId }}</n-avatar>
            <div class="comment-body">
              <div class="comment-head">
                <span class="comment-user">用户 #{{ child.userId }}</span>
                <span class="comment-time font-mono">{{ formatTime(child.createdAt) }}</span>
              </div>
              <!-- eslint-disable-next-line vue/no-v-html -- 经 utils/markdown.ts DOMPurify 消毒 -->
              <div class="comment-content" v-html="renderMarkdown(child.contentMd)"></div>
              <div class="comment-actions">
                <n-popconfirm v-if="canModerate(child)" @positive-click="onDeleteComment(child)">
                  <template #trigger>
                    <GsButton variant="text">删除</GsButton>
                  </template>
                  删除这条回帖？
                </n-popconfirm>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="auth.isLoggedIn" class="reply-editor">
        <n-text v-if="nestedReplyTo" depth="3" class="nested-hint">
          回复 用户 #{{ nestedReplyTo.userId }} 的回帖
          <GsButton variant="text" @click="cancelNestedReply">取消</GsButton>
        </n-text>
        <n-input
          v-model:value="replyText"
          type="textarea"
          placeholder="友善回帖，支持 Markdown"
          :autosize="{ minRows: 3, maxRows: 8 }"
        />
        <GsButton
          variant="primary"
          :disabled="submitting || !replyText.trim()"
          @click="submitReply"
        >
          回帖
        </GsButton>
      </div>
      <p v-else class="login-hint">
        <RouterLink to="/login">登录</RouterLink> 后参与回帖
      </p>
    </section>
  </div>
</template>

<style scoped>
.post-card {
  margin-top: 16px;
  padding: 20px 0 8px;
  border-bottom: 1px solid var(--border);
}

.post-card__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.post-meta {
  min-width: 0;
}

.post-tag-link {
  text-decoration: none;
  color: inherit;
}

.post-time,
.comment-time {
  font-size: 0.75rem;
  color: var(--text-3);
}

.post-content {
  line-height: 1.75;
  word-break: break-word;
  color: var(--text-1);
  max-width: 62ch;
}

.reply-card {
  margin-top: 24px;
}

.reply-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-1);
}

.comment-block {
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
}

.comment {
  display: flex;
  gap: 10px;
}

.comment-avatar {
  background: var(--accent);
  color: var(--accent-text);
  font-size: 12px;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  gap: 12px;
  align-items: baseline;
  font-size: 12px;
}

.comment-user {
  color: var(--text-2);
}

.comment-content {
  margin-top: 4px;
  line-height: 1.6;
  word-break: break-word;
  color: var(--text-1);
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  align-items: center;
}

.nested-list {
  margin: 10px 0 0 42px;
  padding-left: 12px;
  border-left: 2px solid var(--border);
}

@media (max-width: 480px) {
  .nested-list {
    margin-left: 20px;
    padding-left: 8px;
  }
}

.nested {
  padding: 8px 0;
}

.reply-editor {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}

.nested-hint {
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.login-hint {
  margin: 8px 0 0;
  color: var(--text-3);
  font-size: 0.875rem;
}

.login-hint a {
  color: var(--accent);
}

.post-state {
  max-width: 480px;
  margin: 64px auto;
}
</style>

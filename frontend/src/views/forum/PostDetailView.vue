<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  NAvatar,
  NButton,
  NCard,
  NInput,
  NPageHeader,
  NPopconfirm,
  NTag,
  NText,
  useMessage
} from 'naive-ui'
import {
  createComment,
  deleteComment,
  deletePost,
  fetchComments,
  fetchPost,
  type CommentNode,
  type PostDetail
} from '@/api/forum'
import { renderMarkdown } from '@/utils/markdown'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = useMessage()

const post = ref<PostDetail | null>(null)
const comments = ref<CommentNode[]>([])
const notFound = ref(false)
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
  const id = route.params.id as string
  try {
    post.value = await fetchPost(id)
  } catch {
    notFound.value = true
    return
  }
  comments.value = await fetchComments('posts', id)
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
    <n-card title="帖子不存在">
      <p>它可能已被作者删除。</p>
      <RouterLink to="/forum"><n-button>返回论坛</n-button></RouterLink>
    </n-card>
  </div>

  <div v-else class="post-detail">
    <n-page-header :title="post?.title ?? '加载中…'" @back="router.back()" />

    <n-card v-if="post" class="post-card">
      <template #header-extra>
        <n-popconfirm v-if="isOwner" @positive-click="onDeletePost">
          <template #trigger>
            <n-button size="small" type="error">删除</n-button>
          </template>
          删除后所有人不可见，确定吗？
        </n-popconfirm>
      </template>

      <n-space class="post-meta">
        <n-tag size="small" :bordered="false">浏览 {{ post.viewCount }}</n-tag>
        <n-tag size="small" :bordered="false">回帖 {{ post.replyCount }}</n-tag>
        <n-text depth="3" class="post-time">{{ formatTime(post.createdAt) }}</n-text>
      </n-space>

      <!-- eslint-disable-next-line vue/no-v-html -- 经 utils/markdown.ts DOMPurify 消毒（全站唯一出口） -->
      <div class="post-content" v-html="renderedContent"></div>
    </n-card>

    <n-card title="回帖（{{ post?.replyCount ?? 0 }}）" class="reply-card">
      <div v-if="comments.length === 0" class="reply-empty">还没有回帖，来抢沙发</div>

      <div v-for="comment in comments" :key="comment.id" class="comment-block">
        <div class="comment">
          <n-avatar round :size="32" class="comment-avatar">{{ comment.userId }}</n-avatar>
          <div class="comment-body">
            <div class="comment-head">
              <n-text depth="3">用户 #{{ comment.userId }}</n-text>
              <n-text depth="3" class="comment-time">{{ formatTime(comment.createdAt) }}</n-text>
            </div>
            <!-- eslint-disable-next-line vue/no-v-html -- 经 utils/markdown.ts DOMPurify 消毒 -->
            <div class="comment-content" v-html="renderMarkdown(comment.contentMd)"></div>
            <div class="comment-actions">
              <n-button text size="tiny" @click="startNestedReply(comment)">回复</n-button>
              <n-popconfirm v-if="canModerate(comment)" @positive-click="onDeleteComment(comment)">
                <template #trigger>
                  <n-button text size="tiny" type="error">删除</n-button>
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
                <n-text depth="3">用户 #{{ child.userId }}</n-text>
                <n-text depth="3" class="comment-time">{{ formatTime(child.createdAt) }}</n-text>
              </div>
              <!-- eslint-disable-next-line vue/no-v-html -- 经 utils/markdown.ts DOMPurify 消毒 -->
              <div class="comment-content" v-html="renderMarkdown(child.contentMd)"></div>
              <div class="comment-actions">
                <n-popconfirm v-if="canModerate(child)" @positive-click="onDeleteComment(child)">
                  <template #trigger>
                    <n-button text size="tiny" type="error">删除</n-button>
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
          <n-button text size="tiny" @click="cancelNestedReply">取消</n-button>
        </n-text>
        <n-input
          v-model:value="replyText"
          type="textarea"
          placeholder="友善回帖，支持 Markdown"
          :autosize="{ minRows: 3, maxRows: 8 }"
        />
        <n-button type="primary" :loading="submitting" :disabled="!replyText.trim()" @click="submitReply">
          回帖
        </n-button>
      </div>
      <n-text v-else depth="3">
        <RouterLink to="/login">登录</RouterLink> 后参与回帖
      </n-text>
    </n-card>
  </div>
</template>

<style scoped>
.post-card {
  margin-top: 16px;
}

.post-meta {
  margin-bottom: 12px;
}

.post-time {
  font-size: 12px;
}

.post-content {
  line-height: 1.75;
  word-break: break-word;
}

.reply-card {
  margin-top: 16px;
}

.reply-empty {
  color: var(--color-text-secondary);
  padding: 16px 0;
}

.comment-block {
  padding: 12px 0;
  border-bottom: 1px solid var(--color-bg-layout);
}

.comment {
  display: flex;
  gap: 10px;
}

.comment-avatar {
  background: var(--color-primary);
  color: #fff;
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

.comment-time {
  font-size: 12px;
}

.comment-content {
  margin-top: 4px;
  line-height: 1.6;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.nested-list {
  margin: 10px 0 0 42px;
  padding-left: 12px;
  border-left: 2px solid var(--color-bg-layout);
}

.nested {
  padding: 8px 0;
}

.reply-editor {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nested-hint {
  font-size: 12px;
}

.post-state {
  max-width: 480px;
  margin: 64px auto;
}
</style>

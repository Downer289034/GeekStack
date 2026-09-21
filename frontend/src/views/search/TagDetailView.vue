<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NEmpty, NIcon, NPagination } from 'naive-ui'
import {
  ChatbubblesOutline,
  CubeOutline,
  FlashOutline,
  PricetagOutline
} from '@vicons/ionicons5'
import { pagePosts, type PostCard } from '@/api/forum'
import { pageShares, type ShareCard } from '@/api/share'
import { pageSkills, type SkillCard } from '@/api/skill'
import GsButton from '@/components/gs/GsButton.vue'
import GsFeedRow from '@/components/gs/GsFeedRow.vue'
import GsProjectCard from '@/components/gs/GsProjectCard.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import GsTag from '@/components/gs/GsTag.vue'
import GsUnderlineTabs from '@/components/gs/GsUnderlineTabs.vue'

type ContentType = 'post' | 'share' | 'skill'

const PAGE_SIZE = 9
const route = useRoute()
const router = useRouter()

const tagName = computed(() => decodeURIComponent(String(route.params.name ?? '')))

const typeTabs = [
  { key: 'post', label: '帖子' },
  { key: 'share', label: '项目' },
  { key: 'skill', label: 'Skill' }
]

const contentType = ref<ContentType>('share')
const loading = ref(true)
const total = ref(0)
const current = ref(1)
const posts = ref<PostCard[]>([])
const shares = ref<ShareCard[]>([])
const skills = ref<SkillCard[]>([])

function parseType(raw: unknown): ContentType {
  if (raw === 'post' || raw === 'share' || raw === 'skill') return raw
  return 'share'
}

const typeModel = computed({
  get: () => contentType.value,
  set: (v: string) => {
    if (v !== 'post' && v !== 'share' && v !== 'skill') return
    if (v === contentType.value && route.query.type === v) return
    current.value = 1
    void router.replace({
      query: { ...route.query, type: v }
    })
  }
})

const countLabel = computed(() => {
  const n = total.value
  if (contentType.value === 'post') return `共 ${n} 篇帖子`
  if (contentType.value === 'skill') return `共 ${n} 个 Skill`
  return `共 ${n} 个项目`
})

const emptyDescription = computed(() => {
  if (contentType.value === 'post') return '该标签下暂无帖子'
  if (contentType.value === 'skill') return '该标签下暂无 Skill'
  return '该标签下暂无项目'
})

const emptyCta = computed(() => {
  if (contentType.value === 'post') return { to: '/forum/new', label: '去发帖' }
  if (contentType.value === 'skill') return { to: '/skills/new', label: '发布 Skill' }
  return { to: '/shares/new', label: '去发布' }
})

const emptyIcon = computed(() => {
  if (contentType.value === 'post') return ChatbubblesOutline
  if (contentType.value === 'skill') return FlashOutline
  return CubeOutline
})

const listEmpty = computed(() => {
  if (contentType.value === 'post') return posts.value.length === 0
  if (contentType.value === 'skill') return skills.value.length === 0
  return shares.value.length === 0
})

function formatTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

function skillMeta(card: SkillCard): string {
  const parts = [`浏览 ${card.viewCount}`, formatTime(card.createdAt)]
  if (card.likeCount != null) parts.unshift(`赞 ${card.likeCount}`)
  return parts.join(' · ')
}

async function load() {
  if (!tagName.value) return
  loading.value = true
  posts.value = []
  shares.value = []
  skills.value = []
  try {
    if (contentType.value === 'post') {
      const page = await pagePosts(current.value, PAGE_SIZE, undefined, undefined, tagName.value)
      posts.value = page.records
      total.value = page.total
    } else if (contentType.value === 'skill') {
      const page = await pageSkills(current.value, PAGE_SIZE, tagName.value)
      skills.value = page.records
      total.value = page.total
    } else {
      const page = await pageShares(current.value, PAGE_SIZE, tagName.value)
      shares.value = page.records
      total.value = page.total
    }
  } finally {
    loading.value = false
  }
}

function onPageChange(page: number) {
  current.value = page
  void load()
}

watch(
  () => [String(route.params.name ?? ''), parseType(route.query.type)] as const,
  ([name, type], prev) => {
    if (!name) return
    const nameChanged = !prev || prev[0] !== name
    const typeChanged = !prev || prev[1] !== type
    if (!nameChanged && !typeChanged) return
    contentType.value = type
    if (nameChanged || typeChanged) current.value = 1
    void load()
  }
)

onMounted(() => {
  contentType.value = parseType(route.query.type)
  if (!route.query.type) {
    void router.replace({ query: { ...route.query, type: contentType.value } })
  }
  void load()
})
</script>

<template>
  <div class="tag-detail">
    <div class="tag-detail__header">
      <div class="tag-detail__title-row">
        <GsButton variant="text" @click="router.push({ name: 'tags' })">← 标签云</GsButton>
        <h2 class="tag-detail__title font-display">
          <n-icon :component="PricetagOutline" :size="22" />
          <GsTag>{{ tagName }}</GsTag>
        </h2>
      </div>
      <span class="tag-detail__count font-mono">{{ countLabel }}</span>
    </div>

    <GsUnderlineTabs v-model="typeModel" :items="typeTabs" aria-label="内容类型" />

    <GsSkeleton
      v-if="loading"
      :variant="contentType === 'share' ? 'card' : 'row'"
      :count="6"
    />

    <template v-else>
      <n-empty v-if="listEmpty" :description="emptyDescription">
        <template #icon>
          <n-icon :component="emptyIcon" :size="48" />
        </template>
        <template #extra>
          <GsButton variant="primary" :to="emptyCta.to">{{ emptyCta.label }}</GsButton>
        </template>
      </n-empty>

      <div v-else-if="contentType === 'post'" class="feed-list">
        <GsFeedRow
          v-for="post in posts"
          :key="post.id"
          v-reveal
          :title="post.title"
          :to="`/forum/posts/${post.id}`"
          :tags="post.tags ?? []"
          :pinned="post.isPinned === 1"
          :featured="post.isFeatured === 1"
          :meta="`回帖 ${post.replyCount} · 浏览 ${post.viewCount} · ${formatTime(post.createdAt)}`"
        />
      </div>

      <div v-else-if="contentType === 'skill'" class="feed-list">
        <GsFeedRow
          v-for="skill in skills"
          :key="skill.id"
          v-reveal
          :title="skill.title"
          :to="`/skills/${skill.id}`"
          :excerpt="skill.summary"
          :tags="skill.tags ?? []"
          :meta="skillMeta(skill)"
        />
      </div>

      <div v-else class="card-grid">
        <GsProjectCard
          v-for="card in shares"
          :key="card.id"
          v-reveal
          :title="card.title"
          :to="`/shares/${card.id}`"
          :summary="card.summary"
          :tags="card.tags ?? []"
          :view-count="card.viewCount"
          :time="formatTime(card.createdAt)"
        />
      </div>

      <div v-if="!listEmpty" class="pager">
        <n-pagination
          :page="current"
          :page-size="PAGE_SIZE"
          :item-count="total"
          :disabled="loading"
          @update:page="onPageChange"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.tag-detail__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag-detail__title-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.tag-detail__title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.tag-detail__count {
  font-size: 0.75rem;
  color: var(--text-3);
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

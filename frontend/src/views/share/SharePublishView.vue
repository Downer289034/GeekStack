<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NDynamicTags,
  NInput,
  NEmpty,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { createShare, fetchShare, updateShare } from '@/api/share'
import { fetchRepos, type UserRepo } from '@/api/repo'
import GsButton from '@/components/gs/GsButton.vue'
import GsMdEditor from '@/components/gs/GsMdEditor.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const editId = computed(() => {
  const raw = route.params.id
  const id = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isInteger(id) && id > 0 ? id : null
})
const isEdit = computed(() => editId.value != null)

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const loadingShare = ref(isEdit.value)

const showRepoDrawer = ref(false)
const repoLoading = ref(false)
const availableRepos = ref<UserRepo[]>([])

const model = reactive({
  title: '',
  summary: '',
  contentMd: '',
  repoUrl: '',
  demoUrl: '',
  tags: [] as string[]
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入项目名称' },
    { max: 200, message: '标题最长 200 字' }
  ],
  summary: [
    { required: true, message: '请输入一句话介绍' },
    { max: 500, message: '简介最长 500 字' }
  ],
  contentMd: [{ required: true, message: '请输入项目介绍（Markdown）' }],
  repoUrl: [{ type: 'url', message: '仓库地址必须是合法 URL' }],
  demoUrl: [{ type: 'url', message: '在线地址必须是合法 URL' }]
}

async function prefillForEdit() {
  if (editId.value == null) return
  try {
    const share = await fetchShare(editId.value)
    model.title = share.title
    model.summary = share.summary
    model.contentMd = share.contentMd
    model.repoUrl = share.repoUrl ?? ''
    model.demoUrl = share.demoUrl ?? ''
    model.tags = share.tags ?? []
  } finally {
    loadingShare.value = false
  }
}

async function openRepoDrawer() {
  showRepoDrawer.value = true
  repoLoading.value = true
  try {
    availableRepos.value = await fetchRepos(auth.user?.username ?? '', true)
  } catch {
    availableRepos.value = []
  } finally {
    repoLoading.value = false
  }
}

function importRepo(repo: UserRepo) {
  model.repoUrl = repo.repoUrl
  if (!model.title) {
    model.title = repo.repoFullName
  }
  if (!model.summary && repo.description) {
    model.summary = repo.description
  }
  showRepoDrawer.value = false
}

function goAfterSave(id: number) {
  if (isEdit.value) {
    void router.push(`/shares/${id}`)
  } else {
    void router.push('/shares')
  }
}

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!auth.isLoggedIn) {
    void router.push('/login')
    return
  }
  submitting.value = true
  try {
    const payload = {
      title: model.title,
      summary: model.summary,
      contentMd: model.contentMd,
      repoUrl: model.repoUrl || undefined,
      demoUrl: model.demoUrl || undefined,
      tags: model.tags.length > 0 ? model.tags : undefined
    }
    const saved = isEdit.value
      ? await updateShare(editId.value as number, payload)
      : await createShare(payload)
    goAfterSave(saved.id)
  } catch {
    // 错误提示由请求拦截器统一 toast
  } finally {
    submitting.value = false
  }
}

void prefillForEdit()
</script>

<template>
  <div class="publish">
    <h2 class="publish-title font-display">{{ isEdit ? '编辑项目' : '发布项目' }}</h2>
    <GsSkeleton v-if="loadingShare" variant="detail" />
    <n-form v-else ref="formRef" :model="model" :rules="rules" label-placement="top">
        <template v-if="!isEdit && auth.isLoggedIn">
          <GsButton variant="ghost" class="import-btn" @click="openRepoDrawer">
            从我的仓库导入（自动填充名称/描述/仓库地址）
          </GsButton>
        </template>

        <n-form-item label="项目名称" path="title">
          <n-input v-model:value="model.title" placeholder="项目名称" />
        </n-form-item>
        <n-form-item label="一句话介绍" path="summary">
          <n-input v-model:value="model.summary" placeholder="用一句话介绍你的项目" />
        </n-form-item>
        <n-form-item label="技术栈标签（回车添加，最多 10 个）" path="tags">
          <n-dynamic-tags v-model:value="model.tags" :max="10" />
        </n-form-item>
        <n-form-item label="仓库地址（可选）" path="repoUrl">
          <n-input v-model:value="model.repoUrl" placeholder="https://github.com/you/repo" />
        </n-form-item>
        <n-form-item label="在线 Demo 地址（可选，留空提交即清除）" path="demoUrl">
          <n-input v-model:value="model.demoUrl" placeholder="https://your-demo.example.com" />
        </n-form-item>
        <n-form-item label="项目介绍（Markdown）" path="contentMd">
          <GsMdEditor
            v-model="model.contentMd"
            placeholder="用 Markdown 介绍你的项目…"
          />
        </n-form-item>
        <GsButton variant="primary" :disabled="submitting" @click="onSubmit">
          {{ isEdit ? '保存修改' : '发布' }}
        </GsButton>
      </n-form>

    <n-drawer v-model:show="showRepoDrawer" :width="400" placement="right">
      <n-drawer-content title="选择要导入的仓库">
        <GsSkeleton v-if="repoLoading" variant="row" :count="4" />
        <template v-else>
          <n-empty
            v-if="availableRepos.length === 0"
            description="暂无可用仓库（未绑定或快照为空）"
          />
          <button
            v-for="repo in availableRepos"
            :key="repo.id"
            type="button"
            class="repo-pick-item"
            @click="importRepo(repo)"
          >
            <span class="repo-pick-name font-mono">{{ repo.repoFullName }}</span>
            <span class="repo-pick-desc">{{ repo.description || '暂无描述' }}</span>
          </button>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<style scoped>
.publish-title {
  margin: 0 0 20px;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.import-btn {
  margin-bottom: 16px;
  width: 100%;
}

.repo-pick-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 12px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  font-family: inherit;
}

.repo-pick-item:hover {
  background: var(--surface-2);
}

.repo-pick-name {
  display: block;
  font-weight: 500;
  color: var(--accent);
}

.repo-pick-desc {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-3);
}
</style>

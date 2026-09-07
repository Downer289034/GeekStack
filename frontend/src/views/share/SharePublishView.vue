<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NForm,
  NFormItem,
  NDynamicTags,
  NInput,
  NSpin,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createShare, fetchShare, updateShare } from '@/api/share'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 编辑模式：/shares/edit/:id 复用同一表单组件（不复制代码）
const editId = computed(() => {
  const raw = route.params.id
  const id = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isInteger(id) && id > 0 ? id : null
})
const isEdit = computed(() => editId.value != null)

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const loadingShare = ref(isEdit.value)

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

// 精简工具栏：写作场景足够，避免按钮过宽
const toolbar: ToolbarNames[] = ['bold', 'italic', 'strikeThrough', 'title', '-', 'code', 'quote', 'unorderedList', 'link', 'table', '-', 'preview']

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

function goAfterSave(id: number) {
  // 编辑后回详情页确认；发布后回广场
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
    const saved = isEdit.value ? await updateShare(editId.value as number, payload) : await createShare(payload)
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
  <n-card :title="isEdit ? '编辑项目' : '发布项目'">
    <n-spin :show="loadingShare">
      <n-form ref="formRef" :model="model" :rules="rules" label-placement="top">
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
        <n-form-item label="项目介绍（Markdown，实时预览）" path="contentMd">
          <div class="editor-wrap">
            <MdEditor v-model="model.contentMd" :toolbars="toolbar" placeholder="用 Markdown 介绍你的项目…" />
          </div>
        </n-form-item>
        <n-button type="primary" :loading="submitting" @click="onSubmit">
          {{ isEdit ? '保存修改' : '发布' }}
        </n-button>
      </n-form>
    </n-spin>
  </n-card>
</template>

<style scoped>
.editor-wrap {
  width: 100%;
}

.editor-wrap :deep(.md-editor) {
  height: 420px;
}
</style>

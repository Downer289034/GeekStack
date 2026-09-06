<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NForm,
  NFormItem,
  NDynamicTags,
  NInput,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createShare } from '@/api/share'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

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
    await createShare({
      title: model.title,
      summary: model.summary,
      contentMd: model.contentMd,
      repoUrl: model.repoUrl || undefined,
      demoUrl: model.demoUrl || undefined,
      tags: model.tags.length > 0 ? model.tags : undefined
    })
    void router.push('/shares')
  } catch {
    // 错误提示由请求拦截器统一 toast
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <n-card title="发布项目">
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
      <n-form-item label="在线 Demo 地址（可选）" path="demoUrl">
        <n-input v-model:value="model.demoUrl" placeholder="https://your-demo.example.com" />
      </n-form-item>
      <n-form-item label="项目介绍（Markdown，实时预览）" path="contentMd">
        <div class="editor-wrap">
          <MdEditor v-model="model.contentMd" :toolbars="toolbar" placeholder="用 Markdown 介绍你的项目…" />
        </div>
      </n-form-item>
      <n-button type="primary" :loading="submitting" @click="onSubmit">发布</n-button>
    </n-form>
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

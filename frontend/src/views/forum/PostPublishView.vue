<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, type FormInst, type FormRules } from 'naive-ui'
import { createPost, fetchCategories, type Category } from '@/api/forum'
import GsButton from '@/components/gs/GsButton.vue'
import GsMdEditor from '@/components/gs/GsMdEditor.vue'
import GsTag from '@/components/gs/GsTag.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const categories = ref<Category[]>([])
const categoryId = ref<number | null>(null)

const model = reactive({ title: '', contentMd: '' })

const rules: FormRules = {
  categoryId: [{ required: true, type: 'number', message: '请选择分类' }],
  title: [
    { required: true, message: '请输入标题' },
    { max: 200, message: '标题最长 200 字' }
  ],
  contentMd: [{ required: true, message: '请输入正文（Markdown）' }]
}

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!auth.isLoggedIn || categoryId.value == null) {
    return
  }
  submitting.value = true
  try {
    const post = await createPost({
      categoryId: categoryId.value,
      title: model.title,
      contentMd: model.contentMd
    })
    void router.push(`/forum/posts/${post.id}`)
  } catch {
    // 错误提示由请求拦截器统一 toast
  } finally {
    submitting.value = false
  }
}

async function init() {
  categories.value = await fetchCategories()
}

void init()
</script>

<template>
  <div class="publish">
    <h2 class="publish-title font-display">发帖</h2>
    <n-form ref="formRef" :model="model" :rules="rules" label-placement="top">
      <n-form-item label="分类" path="categoryId">
        <div class="cat-select">
          <GsTag
            v-for="cat in categories"
            :key="cat.id"
            interactive
            :selected="categoryId === cat.id"
            @click="categoryId = cat.id"
          >
            {{ cat.name }}
          </GsTag>
        </div>
      </n-form-item>
      <n-form-item label="标题" path="title">
        <n-input v-model:value="model.title" placeholder="一句话说清你的问题或主题" />
      </n-form-item>
      <n-form-item label="正文（Markdown）" path="contentMd">
        <GsMdEditor
          v-model="model.contentMd"
          placeholder="支持 Markdown，问题贴请附上：环境版本 · 复现步骤 · 期望与实际表现"
        />
      </n-form-item>
      <GsButton variant="primary" :disabled="submitting" @click="onSubmit">发布</GsButton>
    </n-form>
  </div>
</template>

<style scoped>
.publish-title {
  margin: 0 0 20px;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.cat-select {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NDynamicTags,
  NForm,
  NFormItem,
  NInput,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { createSkill, fetchSkill, updateSkill } from '@/api/skill'
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
const loadingSkill = ref(isEdit.value)

const model = reactive({
  title: '',
  summary: '',
  contentMd: '',
  modelAdaptationMd: '',
  tags: [] as string[]
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题' },
    { max: 200, message: '标题最长 200 字' }
  ],
  summary: [
    { required: true, message: '请输入摘要' },
    { max: 500, message: '摘要最长 500 字' }
  ],
  contentMd: [{ required: true, message: '请输入提示词正文（Markdown）' }],
  modelAdaptationMd: [{ max: 1000, message: '模型适配说明最长 1000 字' }]
}

async function prefillForEdit() {
  if (editId.value == null) return
  try {
    const skill = await fetchSkill(editId.value)
    model.title = skill.title
    model.summary = skill.summary ?? ''
    model.contentMd = skill.contentMd
    model.modelAdaptationMd = skill.modelAdaptationMd ?? ''
    model.tags = skill.tags ?? []
  } finally {
    loadingSkill.value = false
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
      modelAdaptationMd: model.modelAdaptationMd || undefined,
      tags: model.tags.length > 0 ? model.tags : undefined
    }
    const saved = isEdit.value
      ? await updateSkill(editId.value as number, payload)
      : await createSkill(payload)
    void router.push(`/skills/${saved.id}`)
  } catch {
    // toast by interceptor
  } finally {
    submitting.value = false
  }
}

void prefillForEdit()
</script>

<template>
  <div class="publish">
    <h2 class="publish-title font-display">{{ isEdit ? '编辑 Skill' : '发布 Skill' }}</h2>
    <GsSkeleton v-if="loadingSkill" variant="detail" />
    <n-form v-else ref="formRef" :model="model" :rules="rules" label-placement="top">
      <n-form-item label="标题" path="title">
        <n-input v-model:value="model.title" placeholder="例如：Java 异常分析助手" />
      </n-form-item>
      <n-form-item label="摘要" path="summary">
        <n-input
          v-model:value="model.summary"
          type="textarea"
          :rows="2"
          placeholder="一句话说明这个 Skill 能做什么"
        />
      </n-form-item>
      <n-form-item label="标签（回车添加，最多 5 个）" path="tags">
        <n-dynamic-tags v-model:value="model.tags" :max="5" />
      </n-form-item>
      <n-form-item label="模型适配说明（可选）" path="modelAdaptationMd">
        <n-input
          v-model:value="model.modelAdaptationMd"
          type="textarea"
          :rows="2"
          placeholder="适用模型、温度、上下文长度等注意事项"
        />
      </n-form-item>
      <n-form-item label="提示词正文（Markdown）" path="contentMd">
        <GsMdEditor
          v-model="model.contentMd"
          placeholder="写出可直接复制使用的系统/用户提示词…"
        />
      </n-form-item>
      <GsButton variant="primary" :disabled="submitting" @click="onSubmit">
        {{ isEdit ? '保存修改' : '发布' }}
      </GsButton>
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
</style>

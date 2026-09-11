<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { uploadMedia } from '@/api/media'
import { useThemeStore } from '@/stores/theme'

const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    toolbars?: ToolbarNames[]
    /** 默认单栏编辑；设 true 为分栏预览 */
    preview?: boolean
    disabled?: boolean
    /**
     * 自定义批量上传；默认逐张 POST /api/v1/media/upload?scene=editor。
     * 返回与 files 同序的 URL 列表。
     */
    uploadHandler?: (files: File[]) => Promise<string[]>
  }>(),
  {
    placeholder: '支持 Markdown…\n粘贴或拖入图片可直接上传',
    toolbars: () =>
      [
        'bold',
        'italic',
        'strikeThrough',
        'title',
        '-',
        'code',
        'quote',
        'unorderedList',
        'orderedList',
        'link',
        'image',
        'table',
        '-',
        'preview'
      ] as ToolbarNames[],
    preview: false,
    disabled: false,
    uploadHandler: undefined
  }
)

const emit = defineEmits<{
  'update:modelValue': [string]
}>()

const { theme } = storeToRefs(useThemeStore())

const editorTheme = computed(() => (theme.value === 'dark' ? 'dark' : 'light'))
const codeTheme = computed(() => (theme.value === 'dark' ? 'atom' : 'github'))

function onUpdate(v: string) {
  emit('update:modelValue', v)
}

async function defaultUpload(files: File[]): Promise<string[]> {
  const urls: string[] = []
  for (const file of files) {
    const result = await uploadMedia(file, 'editor')
    urls.push(result.url)
  }
  return urls
}

async function onUploadImg(files: File[], callback: (urls: string[]) => void) {
  try {
    const urls = props.uploadHandler
      ? await props.uploadHandler(files)
      : await defaultUpload(files)
    callback(urls)
  } catch {
    // request 拦截器已 toast；空列表让 md-editor 不插入坏链
    callback([])
  }
}
</script>

<template>
  <div class="gs-md-editor">
    <MdEditor
      :model-value="modelValue"
      :theme="editorTheme"
      :code-theme="codeTheme"
      preview-theme="default"
      :toolbars="toolbars"
      :placeholder="placeholder"
      :preview="preview"
      :disabled="disabled"
      @update:model-value="onUpdate"
      @on-upload-img="onUploadImg"
    />
  </div>
</template>

<style scoped>
.gs-md-editor {
  width: 100%;
}

.gs-md-editor :deep(.md-editor) {
  height: clamp(260px, 55vh, 420px);
  border-radius: var(--radius-small);
}

/*
 * 夜栈灯码 Token 透传（M7-1 spike 结论：覆盖 --md-*，不换库）
 * 同时覆盖 .md-editor / .md-editor-dark，避免库内硬编码色压过。
 */
.gs-md-editor :deep(.md-editor),
.gs-md-editor :deep(.md-editor-dark) {
  --md-color: var(--text-1);
  --md-hover-color: var(--text-2);
  --md-bk-color: var(--surface-1);
  --md-bk-color-outstand: var(--surface-2);
  --md-bk-hover-color: var(--surface-3);
  --md-border-color: var(--border);
  --md-border-hover-color: var(--border-strong);
  --md-border-active-color: var(--accent);
  --md-modal-mask: var(--overlay);
  --md-modal-shadow: var(--shadow-md);
  --md-scrollbar-bg-color: var(--surface-1);
  --md-scrollbar-thumb-color: var(--surface-3);
  --md-scrollbar-thumb-hover-color: var(--border-strong);
  --md-scrollbar-thumb-active-color: var(--text-3);
}

/* 表格形状选择器等少量硬编码色 */
.gs-md-editor :deep(.md-editor-dark .md-editor-table-shape-col-default) {
  background-color: var(--surface-2);
}

.gs-md-editor :deep(.md-editor-dark .md-editor-table-shape-col-include) {
  background-color: var(--surface-3);
}
</style>

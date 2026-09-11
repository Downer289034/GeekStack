<script setup lang="ts">
/**
 * M7-1 spike：验证 md-editor-v3 能否用夜栈灯码 semantic Token 覆盖 --md-*。
 * 路由 /dev/md-editor-spike · 不接入业务，结论见 docs/M7-1-md-editor-主题透传-spike.md
 */
import { computed, ref } from 'vue'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const overrideOn = ref(true)
const text = ref(`# 夜栈灯码 Spike

正文 \`--surface / --text / --accent\` 透传验证。

\`\`\`java
public class GeekStack {
  public static void main(String[] args) {
    System.out.println("代码为灯");
  }
}
\`\`\`
`)

const toolbar: ToolbarNames[] = [
  'bold',
  'italic',
  'title',
  '-',
  'code',
  'quote',
  'unorderedList',
  'link',
  '-',
  'preview'
]

const editorTheme = computed(() => (themeStore.theme === 'dark' ? 'dark' : 'light'))
const codeTheme = computed(() => (themeStore.theme === 'dark' ? 'atom' : 'github'))
</script>

<template>
  <div class="spike" :class="{ 'is-override': overrideOn }">
    <header class="spike-head">
      <h1 class="font-display">md-editor-v3 × 夜栈灯码 Token 透传 Spike</h1>
      <p class="spike-desc">
        左侧对照：库默认 <code>theme</code>；开启覆盖后把
        <code>--md-*</code> 绑到 <code>--surface / --text / --border / --accent</code>。
      </p>
      <div class="spike-actions">
        <label class="spike-toggle">
          <input v-model="overrideOn" type="checkbox" />
          Token 覆盖
        </label>
        <span class="spike-meta font-mono">
          data-theme={{ themeStore.theme }} · MdEditor theme={{ editorTheme }}
        </span>
      </div>
    </header>

    <div class="editor-wrap">
      <MdEditor
        v-model="text"
        :theme="editorTheme"
        :code-theme="codeTheme"
        preview-theme="default"
        :toolbars="toolbar"
        placeholder="spike…"
      />
    </div>
  </div>
</template>

<style scoped>
.spike {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 0 48px;
}

.spike-head {
  margin-bottom: 20px;
}

.spike-head h1 {
  margin: 0 0 8px;
  font-size: 1.333rem;
  color: var(--text-1);
}

.spike-desc {
  margin: 0 0 12px;
  color: var(--text-2);
  font-size: 0.925rem;
  line-height: 1.6;
}

.spike-desc code {
  font-family: var(--font-mono);
  font-size: 0.85em;
  color: var(--text-1);
}

.spike-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.spike-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  color: var(--text-1);
  cursor: pointer;
}

.spike-meta {
  font-size: 0.78rem;
  color: var(--text-3);
}

.editor-wrap {
  width: 100%;
}

.editor-wrap :deep(.md-editor) {
  height: 420px;
}

/*
 * Spike 覆盖层：把库内 14 个 --md-* 绑到夜栈灯码 semantic。
 * 选择器同时覆盖 light 根与 .md-editor-dark，避免库内硬编码暗色变量压过。
 */
.spike.is-override :deep(.md-editor),
.spike.is-override :deep(.md-editor-dark),
.spike.is-override :deep(.md-editor-modal-container) {
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
</style>

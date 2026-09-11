# M7-1 Spike：md-editor-v3 能否透传夜栈灯码 Token

> 日期：2026-09-11 · 库版本：`md-editor-v3@6.5.6`  
> 规范依据：`docs/geekstack-editor-design-spec.md` §2.1  
> 验证页：`/dev/md-editor-spike`（不进业务发布页）  
> 取证：`docs/assets/m7-1-spike/`

---

## 结论（先报，再动大工程）

**能透传。建议：保留 md-editor-v3，做覆盖，不换库。**

| 选项 | 判定 |
|---|---|
| 覆盖（`theme` 绑定 `data-theme` + 14 个 `--md-*` ← semantic Token） | **采用** |
| 换库（vditor / bytemd / 自研） | **不做**。库已有完整 CSS 变量层；换库成本高于覆盖，且图片粘贴/工具栏/预览已在线上跑通 |

**一句话**：暗色下把 `--md-bk-color` 从库默认 `#000` 绑到 `--surface-1`（`oklch(16% 0.018 262)`）实测成功；浅色绑到 `--surface-1`（`#ffffff`）同样成功。P0「白底亮块」的根因是业务页 **未传 `theme`**（默认 light），不是变量透传做不到。

---

## 验证方法

1. 读库 CSS：`md-editor-v3/lib/style.css` 共 **14** 个 `--md-*`，暗色写在 `.md-editor-dark`。
2. 官方 API：`theme: 'light' | 'dark'`、`previewTheme`、`codeTheme`（highlight.js 成对 CSS）。
3. Spike 页四格对照：`dark/light` × `覆盖关/开`，Playwright 抽计算样式。

### 实测（`spike-report.json`）

| 场景 | `--md-bk-color` | 编辑器背景 | Token 对齐 |
|---|---|---|---|
| Dark 默认 | `#000` | `rgb(0,0,0)` | 否（库黑，非墨蓝） |
| **Dark 覆盖** | `oklch(16% 0.018 262)` | 同 `--surface-1` | **是** |
| Light 默认 | `#fff` | `rgb(255,255,255)` | 否（库白） |
| **Light 覆盖** | `#ffffff` | 同 `--surface-1` | **是** |
| 模拟业务页（站点 Dark + 编辑器默认 light） | `#fff` | **白底亮块** | 否 |

截图：`spike-dark-default.png` / `spike-dark-override.png` / `spike-light-*.png` / `m6-6-editor-dark-site-light-editor.png`

---

## 覆盖清单（若批准落地，不在本 spike 改业务）

把 `--md-*` 绑到语义层即可，不必改库源码：

```css
.md-editor, .md-editor-dark, .md-editor-modal-container {
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
```

组件侧必做：`theme` ← `data-theme`（dark/light）；`codeTheme` 夜 `atom` / 日 `github`。

---

## 残余风险（覆盖仍要处理，但不够换库）

1. **预览皮肤** `@vavt/markdown-theme` 独立于 `--md-*`，需自定义 `previewTheme` 或与详情页 `.prose` 对齐。
2. **代码高亮** 走 highlight.js CDN，必须成对切 light/dark。
3. **少量硬编码**（如表格选择器 `#222` / `#555`）要用 `:deep` 补刀。
4. **CodeMirror 语法色** 不完全吃 `--md-*`，可接受为编辑态语法色，不挡 P0 表面色。

---

## 业务页现状（M6-6 编辑器补巡，见巡检报告附录）

`PostPublishView` / `SharePublishView` 均 **未传 `theme`** → 夜间站点上编辑器默认 light → 规范 §2.1 白底亮块。假 token 无法过登录守卫，用 spike 页强制 light 变量复现。

---

## 建议的 M7-1 落地范围（待拍板后再动）

1. 封装 `<GsMdEditor />`：Token 覆盖 + `theme`/`codeTheme` 绑定 + 默认「编辑」单栏。
2. 替换发帖 / 发项目两处 `MdEditor`。
3. 预览样式对齐详情正文；硬编码色补覆盖。
4. **不做**：换库、草稿/粘贴上传/速查浮层（属规范 §3–4，可拆 M7-2）。

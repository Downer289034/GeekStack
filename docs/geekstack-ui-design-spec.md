# 极客栈 GeekStack · UI 设计方案（前端开发交接版）

> 设计概念：**夜栈灯码** —— 深墨蓝底 × 琥珀灯笼强调色，呼应「客栈」意象
> 视觉参考稿：`docs/geekstack-ui-design.html`（单文件，浏览器直接打开，右上角可切换日/夜主题）
> 版本：v1.1（去 AI 味修正版）｜ 日期：2026-09-11 ｜ 技术栈基线：Vue3 + Naive UI

---

## 1. 设计方向

| 项 | 结论 |
|---|---|
| 风格 | 现代科技感，编辑部式非对称布局，克制用色 |
| 记忆点 | 墨蓝 × 琥珀灯笼，**全站仅一个强调色**，禁止引入第二种彩色主题色 |
| 布局语言 | 信息流为「列表行」而非卡片堆叠；玻璃拟态（backdrop-blur）**只允许用于顶栏**一处 |
| 禁止项 | 青色/紫蓝渐变、霓虹发光、纯黑 `#000`、纯灰（不带色偏的中性色）、万物皆卡片 |

## 2. 色彩系统（OKLCH，两层 Token 架构）

**架构规则：primitive token 固定不动；dark/light 只重定义 semantic 层。**

### 2.1 Primitive（固定值）

```css
/* 墨蓝中性阶（暖偏染，非纯灰） */
--ink-950: oklch(13% 0.015 262);
--ink-900: oklch(16% 0.018 262);
--ink-800: oklch(21% 0.02 262);
--ink-700: oklch(28% 0.02 262);
--ink-500: oklch(52% 0.015 262);
--ink-300: oklch(72% 0.01 262);
--ink-100: oklch(93% 0.008 262);

/* 琥珀灯笼（唯一强调色） */
--amber-300: oklch(86% 0.1 82);
--amber-500: oklch(76% 0.15 75);
--amber-600: oklch(68% 0.15 70);

/* 暖纸中性阶（浅色主题） */
--paper-50: oklch(97.5% 0.008 80);
--paper-100: oklch(95% 0.01 80);
--paper-200: oklch(90% 0.012 80);
--paper-300: oklch(82% 0.012 80);
--paper-700: oklch(38% 0.015 75);
--paper-900: oklch(22% 0.018 75);

/* 语义色 */
--success: oklch(70% 0.14 150);
--warning: oklch(78% 0.14 85);
--error:   oklch(62% 0.19 25);
--info:    oklch(70% 0.11 230);
```

### 2.2 Semantic（随主题切换）

| Token | Dark（默认） | Light（晨纸） |
|---|---|---|
| `--bg` | `--ink-950` | `--paper-50` |
| `--surface-1` | `--ink-900` | `#ffffff` |
| `--surface-2` | `--ink-800` | `--paper-100` |
| `--surface-3` | `--ink-700` | `--paper-200` |
| `--border` | `oklch(28% 0.02 262 / .9)` | `--paper-200` |
| `--text-1` | `--ink-100` | `--paper-900` |
| `--text-2` | `--ink-300` | `--paper-700` |
| `--text-3` | `--ink-500` | `oklch(55% 0.015 75)` |
| `--accent` | `--amber-500` | `--amber-600`（加深保对比） |
| `--accent-text` | `--ink-950` | `#ffffff` |
| `--nav-bg` | `oklch(16% 0.018 262 / .82)` | `oklch(97.5% 0.008 80 / .85)` |

**落地要点**：通过 `<html data-theme="dark|light">` 切换；Naive UI 暗色主题用 `darkTheme` + `themeOverrides` 映射以上 semantic token；正文字重 dark 模式用 380、light 模式用 400；深底浅字行高加 0.05–0.1。

## 3. 字体系统

| 角色 | 字体栈 |
|---|---|
| 展示/标题（拉丁） | `'Space Grotesk', 'PingFang SC', 'Microsoft YaHei', sans-serif` |
| 正文/UI（中文） | `-apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif` |
| 代码/元数据 | `'JetBrains Mono', 'Cascadia Code', Consolas, monospace`（`font-variant-ligatures: none`） |

**5 级字阶（比例 1.333），全站不得私自新增字号：**

| 级 | 尺寸/行高 | 用途 |
|---|---|---|
| xl | `clamp(2.2rem, 4.5vw+0.6rem, 3.6rem)` / 1.18 | Hero 展示标题（仅营销区用流体，产品 UI 用固定值） |
| lg | 1.333rem / 1.4 | 区块标题、文章头条标题 |
| base | 1rem / 1.7 | 正文（中文底线上限，不许再小） |
| sm | 0.875rem / 1.5–1.6 | 辅助信息、摘要 |
| xs | 0.75rem / 1.5 | 元数据、标签（mono 常配 letter-spacing 0.18em + uppercase） |

数字一律 `font-variant-numeric: tabular-nums`（热度、排名、统计）。

## 4. 间距与圆角

- **垂直节奏以正文行高为基**：base 行高 1.7×16px ≈ 27px，区块 padding 取 8 的倍数（20 / 24 / 32 / 48 / 72）
- 圆角：按钮/输入 10px，标签/徽章/小件 6px，Logo 8px，圆点分隔符保留圆形；**不再使用胶囊（999px）造型**
- 容器：`max-width: 1180px`，左右 padding 24px；主栏 `minmax(0, 1fr)` + 右栏 320px，间距 48px

## 5. 组件规格

### 5.1 按钮
| 变体 | 规格 |
|---|---|
| Primary | 琥珀底 + `--accent-text` 字，hover 加深为 `--accent-hover` + `translateY(-1px)` + 琥珀投影 |
| Ghost | 透明底 + `--border-strong` 描边，hover 边框与文字转琥珀 |
| Text | 琥珀色文字，hover 加深 |
| 禁用态 | `opacity: .6; pointer-events: none`（用 disabled 属性，不靠样式判定） |

全站只允许 **一个 Primary**（如「写文章」），feed 区域的操作一律 Ghost/Text。

### 5.2 信息流（核心组件）
- **头条**：纯排版无封面——标签行 + 大标题（1.55rem）+ 摘要（max-width 62ch）+ byline；hover 标题转琥珀
- **列表行**：排名数字（Space Grotesk 700，统一 `--text-3`，不做前几名染色）+ 标题 + 一行摘要 + byline；hover `padding-left: 8px` 平移（transition 0.2s cubic-bezier(0.25,1,0.5,1)），行间用 1px `--border` 分隔线
- **Tab 过滤**：下划线式 Tab（无底色胶囊），选中项 600 字重 + 2px 琥珀底边；过滤逻辑见参考稿 JS（`data-cat` 属性匹配），空态文案「这个分类暂时还没有新内容，去写第一篇吧。」

### 5.3 标签 / 徽章
- Tag：中性描边样式（1px `--border` + `--text-2` 字），hover 才转琥珀描边与文字；**默认态不占用强调色**
- 徽章：语义色 15% alpha 底 + 语义色字（已采纳/审核中/已驳回/置顶），仅用于功能性状态
- Code chip：mono 0.72rem + surface-2 底 + border，圆角 6px

### 5.4 状态设计（必须实现，不可省）
| 状态 | 要求 |
|---|---|
| Loading | 骨架屏（surface-2 色块 + 微呼吸），不做 spinner 转圈 |
| Empty | 有教育意义的引导文案 + 行动按钮（如「去写第一篇」） |
| Error | `--error` 图标 + 具体原因 + 重试按钮，禁止只报「出错了」 |
| Focus | `outline: 2px solid var(--accent); outline-offset: 2px`，全站统一 |

### 5.5 Hero 大字 + 打字动效（v1.1 新增，首页专用）

**大字规格**：`clamp(2.2rem, 4.5vw + 0.6rem, 3.6rem)`，Space Grotesk 700，行高 1.18，字距 -0.015em，`min-height: 2.36em` 预留两行（打字过程中布局不得跳动）。

**打字动效参数**（参考实现：`docs/geekstack-hero-typing.html` / `<HeroTyping />`）：
| 参数 | 值 | 说明 |
|---|---|---|
| START_DELAY | 400ms | 页面加载后启动延迟 |
| CHAR_MS | 165ms/字 | 中文「敲代码」手感区间 150~180ms |
| 分段停顿 | 220ms（换行）/ 60ms（分段） | 模拟换气，避免匀速机械感 |
| 光标 | 3px × 0.85em 琥珀竖条，`steps(1)` 1s 闪烁 | 打完保留全文，光标持续闪，不清空重打 |
| 重播 | Hero 角落「重播动画」 | 手动再打一遍；`prefers-reduced-motion` 用户的唯一播放入口 |
| 文案分段 | `代码为灯，`↵`技术为栈——欢迎`+em(夜宿极客栈)+`。` | em 段渲染为琥珀色 |

**无障碍红线**：`h1` 必须带 `aria-label` 完整文本；打字区容器 `aria-hidden="true"`（避免读屏读出残缺句）；`prefers-reduced-motion: reduce` 时**首屏直接渲染全文、隐藏光标、不自动播**。角落「重播动画」可手动触发一次打字（尊重偏好：默认仍静，效果一键可达）。评审演示可用 `?motion=force`。

## 6. 动效规范

- **缓动**：一律指数族 `cubic-bezier(0.25,1,0.5,1)` / `cubic-bezier(0.16,1,0.3,1)`；**禁止** bounce/elastic 回弹
- **时长**：hover/色彩 150ms；位移/入场 200–600ms
- **只动画 transform 与 opacity**，禁止动画 layout 属性（width/height/top/margin）
- 入场：IntersectionObserver 触发 `translateY(14px)→0 + opacity`，阈值 0.12，一次性
- 全局尊重 `prefers-reduced-motion: reduce`（参考稿已内置关闭逻辑，直接照搬）

## 7. 响应式

| 断点 | 行为 |
|---|---|
| >960px | 双栏（信息流 + 右栏 320px） |
| ≤960px | 单栏，右栏下移（order: 2）；隐藏导航链接与搜索框；头条封面变 16:6 |
| ≤560px | Hero padding 收紧，统计分隔线隐藏 |

容器查询（`@container`）优先于媒体查询做组件级自适应（卡片、信息流行）。

## 8. 可访问性红线（验收必查）

1. 对比度 ≥ 4.5:1（正文）/ 3:1（大字与 UI 组件）；占位符文字同样达标
2. 键盘可达：全部交互元素有 `:focus-visible` 琥珀焦点环；Tab 顺序与视觉一致
3. 触控目标 ≥ 44px（文字链用 padding 或 line-height 撑起）
4. 不禁缩放（禁用 `user-scalable=no`）；200% 缩放不破版
5. 图标按钮必带 `aria-label`；Tab 组用 `role="tablist/tab"` + `aria-selected`
6. 颜色不作为唯一信息载体（排名同时有数字、徽章同时有文字）

## 9. 页面结构与交付基线

**首页信息架构**（参考稿已实现，前端照此结构还原）：
```
顶栏（sticky, 实色 + 1px 底边线）: Logo｜导航(首页/文章/问答/专栏/AI实验室)｜搜索(Ctrl K)｜主题切换｜写文章｜头像
Hero: mono 小注 + 大标题(琥珀点睛词) + lead + mono 数据行
主区: 左=客栈动态(纯排版头条+排名列表+下划线Tab过滤)  右=客栈热榜/热门话题/活跃掌柜
设计系统展示区 + Footer
```

**前端实现建议**：
- Token 层用 CSS Variables 落地（参考稿 `:root` / `[data-theme="light"]` 直接可用），Naive UI 主题通过 `themeOverrides` 映射同一套值
- Space Grotesk 用 `font-display: swap` + fallback 度量对齐，避免 FOUT 跳版
- 验收时逐条过第 5.4 状态表 + 第 8 节红线；每页需跑一次对比度抽检与键盘 Tab 巡检

---

**UI Designer** ｜ 设计系统版本 v1.1 ｜ 2026-09-11
参考稿（视觉还原基准）：`docs/geekstack-ui-design.html`

---

## 附：v1.1 修正记录（针对「AI 味重」的用户反馈）

逐条对照 AI 惯用界面指纹（AI Slop Detection）自查后修正，前端按本版本执行，勿参考 v1.0 视觉记忆：

| # | 问题（v1.0） | 修正（v1.1） |
|---|---|---|
| 1 | Hero 右上角琥珀径向光斑 | 删除，Hero 纯排版 |
| 2 | 4 项大数字统计条 + 竖分隔线（英雄指标模板） | 改为一行 mono 数据注脚 |
| 3 | Logo 琥珀发光投影、按钮 glow 阴影 | 全部移除，扁平化 |
| 4 | 头条封面墨蓝→琥珀渐变 | 删封面，改纯排版大标题 |
| 5 | 顶栏玻璃拟态（backdrop-blur） | 改实色 + 1px 底边线，全站禁用 blur |
| 6 | 琥珀出现 10+ 处（kicker/标签/排名/关注按钮等） | 收缩到 4 处：Logo、标题点睛词、主按钮、交互反馈（hover/焦点） |
| 7 | 标签/徽章全员胶囊（999px）+ Tab 胶囊组 | 标签/徽章 6px 方角描边；Tab 改下划线式 |
| 8 | 入场动效 14px 位移 | 收敛至 10px，时长 600ms→500ms |

**强调色使用规则（新增）**：琥珀只允许出现在 Logo、Hero 标题点睛词、Primary 按钮、交互反馈态（hover/选中下划线/焦点环）四处。标签默认中性描边，排名数字不做染色。判断口诀：**琥珀是灯，不是墙漆——照亮该照亮的地方，不刷满整间屋子。**

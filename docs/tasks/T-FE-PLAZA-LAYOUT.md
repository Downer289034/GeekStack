# T-FE-PLAZA-LAYOUT · 项目广场全宽布局计划

> 依据：新 UI 稿（全宽三列广场，无两侧黑边）  
> 现状问题：`MainLayout` 的 `.gs-shell-main` 固定 `max-width: var(--container-max)`（1180px）居中 → 宽屏两侧大块 `--bg` 黑边，内容挤在中间。  
> 负责人：**开发 B（前端）**｜派发：2026-09-20  
> 范围：先落地 **项目广场**（`/shares`）；Skill 广场可复用同一壳策略，本单不强制改论坛长文页。

---

## 1. 目标观感（对照新稿）

1. **主内容区横向拉宽**：广场页不再困在 1180 中栏；宽屏三列卡片舒展，两侧黑边消失或缩到合理 gutter。  
2. **页头信息架构**：左标题「项目广场」+ 副标题一句；右「共 N 个项目」+ 标签筛选。  
3. **卡片**：封面首字母色块 + 浏览数 + **标题 + 一句话 summary** + 标签 + 时间（新稿有 summary，当前 `GsProjectCard` 未展示）。  
4. **氛围**：暗色底 + 可选轻网格纹理（克制，不抢内容）。  
5. **导航/页脚**：顶栏仍全宽；页脚链接区可继续用 `gs-container`，不要求与广场同宽。

---

## 2. 根因与布局策略

| 层 | 现状 | 计划 |
|---|---|---|
| `.gs-shell-main` | 全局 `max-width: 1180px` | **按路由放宽**：广场类路由用 `wide`（建议 `min(1440px, 100%)` 或 `100%` + 水平 padding）；详情/发帖/论坛长文保持 `md/sm` |
| 导航 | 已全宽 + 内层 `gs-container` | 保持；内层可同步提高到 `1440` 以免顶栏比内容窄一截 |
| `--container-max` | 1180 | 可增至 **1440** 作全站默认；或仅广场 `wide`，避免首页双栏被撑得过散——**优先方案：仅广场/Skill 列表 `contentWidth='wide'`，token 增加 `--container-wide: 1440px`，默认 1180 不动** |

推荐实现（改动面最小）：

1. `MainLayout` 增加 `contentWidth: 'wide' | 'md' | 'sm' | 'default'`。  
2. 路由 meta：`share-plaza`、`skill-plaza`（可选）→ `wide`。  
3. CSS：`.gs-shell-main--wide { max-width: var(--container-wide); }`，左右 `padding` 保持 ≥24px（大屏可 32–40px）。

**禁止**：把整站所有页一刀切 `max-width: none`（详情长文行长会炸可读性）。

---

## 3. 页面结构（SharePlazaView）

```
plaza (wide shell)
├── plaza-header
│   ├── left: title + subtitle
│   └── right: count + tag filter
├── card-grid (3 / 2 / 1)
└── pagination
```

- 副标题文案（可配置常量）：「探索开源 AI 项目、Agent 框架、向量数据库与 LLM 工具库」  
- `pageShares` 已有 `summary` 字段则传入卡片；无则卡片省略 summary 行，不造假数据。  
- 网格：维持 `repeat(3, 1fr)`，断点 1200→2、768→1；`gap` 可略增至 20–24px 配合加宽。

---

## 4. 组件改动

### GsProjectCard

- 新增可选 prop：`summary?: string`（一行，两行截断 `line-clamp: 2`）。  
- 封面：可按标题 hash 在 2–3 档 `surface`/色相间轮换（克制，避免彩虹），对齐新稿「各卡封面色略有差别」。  
- 不引入真实封面图依赖（除非已有 `coverUrl` 且后端有值——有则优先图，无则字母块）。

### SharePlazaView

- 头部重排对齐 §3。  
- 去掉「featured 仅第一张琥珀」若与新稿冲突可保留轻微差异，但不要只有一张特别高。

### SkillPlazaView（可选同单或 follow-up）

- 同一 `wide` meta + 同类页头，保证两广场一致。

---

## 5. 明确不做

- 不改登录页全屏构图。  
- 不改首页 `gs-main-grid` 双栏比例（除非冒烟发现顶栏/主区错位再另单）。  
- 不做新设计系统色板大换血；沿用现有 token。  
- 不引入卡片阴影堆叠、紫光、圆角药丸堆。

---

## 6. 验收

1. ≥1440 宽屏截图：项目广场 **两侧无明显大黑边**，三列卡片占满主内容宽。  
2. 1180 / 768 / 390 断点不炸版；筛选与分页仍可用。  
3. 卡片可见 summary（有数据时）；点击进详情正常。  
4. 顶栏导航单行不回退（nowrap 回归）。  
5. 证据入 `docs/assets/t-fe-plaza-layout/`（宽屏 before 可选 / after 必填）。

---

## 7. 工作量与顺序

1. token + MainLayout `wide`（约 0.5h）  
2. SharePlazaView 页头（约 0.5h）  
3. GsProjectCard summary + 封面微调（约 1h）  
4. Skill 广场对齐（可选 +0.5h）  
5. 截图证据 + 自测

预计 **半天内可关**。

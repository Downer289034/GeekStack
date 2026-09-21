# T-FE-FEATURE-PIN-BADGE · 帖子列表/详情展示精华与置顶

> 负责人：**开发 B（极客栈前端）**  
> 优先级：P2  
> 派发：2026-09-21（T-FE-TAG-DETAIL-TABS 关单后）  
> 依据：后端 `is_featured` / `is_pinned` 已由 T-ADMIN-FEATURE 落地；管家 MA3 可写回

## 背景

加精/置顶 Admin API 已通，但用户侧列表/详情对 **精华（isFeatured）** 展示可能不完整（置顶部分页面已有）。需要用户看得见运营处置结果。

## 范围

1. 论坛列表、帖子详情、（若首页头条用同一 PostCard）对 `isPinned===1` / `isFeatured===1` 展示清晰徽章（「置顶」「精华」），风格跟现有 GsBadge/GsTag 一致。  
2. 搜索结果帖子行若已有 pinned prop，补 featured。  
3. 截图：同一帖加精或置顶前后对比，或列表中带徽章条目，入 `docs/assets/t-fe-feature-pin-badge/`。  
4. 若本地无加精帖：可用已有测试数据或等 A 的 live smoke 帖；汇报注明所用 postId。

## 非范围

- 不做 Admin 操作台（用户侧只读展示）  
- 不改后端字段  
- 不做仓库手动同步（无现成公开 sync API，另单）

## 验收

1. 肉眼能区分置顶 vs 精华  
2. 证据截图 + 独立 commit  
3. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。

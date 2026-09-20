# 顶栏导航文字积压修复

> 2026-09-20 ｜ 开发 B ｜ `MainLayout` 导航单行

## 根因

`.gs-nav__links` 可被压缩（`flex:1; min-width:0`），且 `.gs-nav__link` 无 `white-space: nowrap`；搜索页再叠顶栏搜索框时，中文标签被挤成竖排换行。

## 修复

1. 导航链接 `white-space: nowrap` + `flex-shrink: 0`
2. 搜索 / 标签页：顶栏搜索改为**图标入口**（页内已有搜索），其它页保留完整搜索框
3. ≤1200 / ≤1024 时搜索框逐步收缩，给导航让宽

## 证据

| 文件 | 说明 |
|------|------|
| `01-before-search-squeezed.png` | 修复前：登录态 + 搜索页，链接高 65（换行） |
| `02-after-search-squeezed.png` | 修复后：同条件，链接高 44、nowrap |
| `02-after-search-1280.png` / `1024` | 搜索页桌面宽度 |
| `03-after-home-*` / `04-after-forum-*` / `05-after-shares-*` | 首页/论坛/广场抽查 |

`metrics` 要点（挤扁对照）：before `scrollH=65 whiteSpace=normal` → after `scrollH=44 whiteSpace=nowrap`。

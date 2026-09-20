# 顶栏导航文字积压修复

> 2026-09-20 ｜ 开发 B ｜ `MainLayout` 导航 + 右侧操作单行

## 根因

1. `.gs-nav__links` 可被压缩，且链接无 `white-space: nowrap` → 中文导航竖排。
2. 右侧「写文章 / 设置 / 退出」同理：无 nowrap、可被 flex 挤扁；搜索页双搜索框加剧抢宽。

## 修复

1. 导航链接 `white-space: nowrap` + `flex-shrink: 0`
2. 搜索 / 标签页：顶栏搜索改为**图标入口**；其它页保留完整搜索框
3. ≤1200 / ≤1024 时搜索框逐步收缩，给文字让宽
4. 右侧包一层 `.gs-nav__actions`（整体 `flex-shrink: 0`）；`.gs-text-btn` / `GsButton` 补 `nowrap` + `flex-shrink: 0`

## 证据

| 文件 | 说明 |
|------|------|
| `01-before-search-squeezed.png` | 修复前导航挤扁（高 65） |
| `02-after-search-squeezed.png` | 修复后导航单行（高 44） |
| `07-after-actions-home-1280.png` / `1024` | **登录态首页**：写文章/设置/退出横排 |
| `08-after-actions-search-1280.png` / `1024` | 登录态搜索页整栏无积压 |
| `actions-metrics.json` | 右侧三键 `scrollH=44` + `nowrap` |

验收：首页与搜索页，≥1280 / 1024，导航五项 + 右侧操作均横排可读。

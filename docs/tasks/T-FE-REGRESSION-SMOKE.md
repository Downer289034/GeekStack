# T-FE-REGRESSION-SMOKE · 近期改动浏览器回归包

> 负责人：**开发 B（极客栈前端）**  
> 优先级：P2  
> 派发：2026-09-21（T-FE-REPO-SYNC-LIVE 关单后）

## 背景

近几日连续落地：搜索/助手文案、标签云含帖、标签详情三 Tab、精华/置顶徽章、仓库同步按钮。需一份轻量回归证据，避免互相踩回归。

## 范围

用本机前端 + 现行 API（:8080 或 Vite 代理）走一遍并截图入 `docs/assets/t-fe-regression-smoke/`：

1. `/search` 标题「找内容」+ 助手「社区问答」可区分  
2. `/tags` hint 含帖；点某标签三 Tab 可切  
3. 论坛列表可见置顶/精华徽章（可用既有 mock/真实帖）  
4. Owner 主页可见「同步 Gitee/GitHub」按钮（不强制再同步）  
5. `lint` / `vue-tsc` / `build` 三绿  

附 `README.md` + `result.json`（pass/fail）。

## 非范围

- 不新开功能  
- 不强制等 prod 镜像（prod 冒烟等 A 关镜像后再说）  
- 不改后端

## 验收

1. 上述 1～4 有截图或 JSON 断言  
2. 三绿；独立 commit（仅 docs/assets + 可选脚本）  
3. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。

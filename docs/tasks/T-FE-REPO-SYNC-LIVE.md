# T-FE-REPO-SYNC-LIVE · 真环境同步按钮冒烟

> 负责人：**开发 B（极客栈前端）**  
> 优先级：P2  
> 派发：2026-09-21（T-FE-REPO-SYNC-BTN 关单后）  
> 阻塞：开发 A `T-DEPLOY-BACKEND-SYNC-IMAGE`（未登录 POST sync → 401 证明路由在镜像内）

## 目标

mock 证据已关 UI 单；本单对 **真实后端**（`172.25.41.137` 或你们现行前端所连 API）点一次「同步 GitHub/Gitee」，确认非 404。

## 范围

1. 确认运行中 backend 已含 `2f63e92`（A 汇报 image 后开始）。  
2. 用已绑定 OAuth 的真实/测试账号：Owner 主页点同步 → 成功 toast 或可读错误（429/503 可接受，**不能是路由 404**）。  
3. 截图 1～2 张入 `docs/assets/t-fe-repo-sync-live/`（含 Network 状态码或 toast）。

## 非范围

- 不改代码（除非真机发现必须修的小 bug）  
- 不做 OAuth 重新授权大流程

## 验收

1. 真机请求打到 `/api/v1/me/repos/sync`，非 404  
2. 证据 + 三句话汇报（可无新 commit）

## 汇报格式

做了什么 / 证据 / 疑问。

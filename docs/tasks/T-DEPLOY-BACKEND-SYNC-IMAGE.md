# T-DEPLOY-BACKEND-SYNC-IMAGE · 镜像含手动同步 API

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P1  
> 派发：2026-09-21（T-REPO-MANUAL-SYNC-API 关单后）

## 背景

`2f63e92` 已入 main，但运行中 `geekstack/backend:latest` 若仍停在更早点，B 的「同步仓库」打到 prod/nginx 会 **404**。需重建滚动。

## 范围

1. 按含 `2f63e92`（或更新）的 commit **重建并滚动** `geekstack/backend:latest`；写清 `DEPLOY_COMMIT`。  
2. 冒烟（脱敏）：  
   - health UP  
   - 未登录 `POST /api/v1/me/repos/sync?provider=GITHUB` → **401**（路由在镜像内，非 40400）  
3. 仍遵守：禁止 `docker cp` 热更 jar。

## 非范围

- 不改业务代码  
- 不强制重建 frontend

## 验收

1. 汇报含 image id / `DEPLOY_COMMIT`  
2. 未登录 sync → 401  
3. 三句话汇报

## 完成说明（开发 A，2026-09-21）

- 按含 `2f63e92` 的 HEAD 重建滚动 `geekstack/backend:latest`（无 `docker cp`）
- `DEPLOY_COMMIT=c868d87`；image `ca10eb65197d`
- 冒烟：health UP；未登录 `POST /api/v1/me/repos/sync?provider=GITHUB` → **401** / `code=40100`

## 汇报格式

做了什么 / 证据 / 疑问。

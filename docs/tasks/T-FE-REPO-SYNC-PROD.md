# T-FE-REPO-SYNC-PROD · prod 网关同步冒烟

> 负责人：**开发 B（极客栈前端）**  
> 优先级：P2  
> 派发：2026-09-21（T-FE-REGRESSION-SMOKE 关单后）

## 背景

本机真 API 已冒烟通过。PM 探针：`POST http://172.25.41.137/api/v1/me/repos/sync?provider=GITHUB` 未登录 → **401**（路由已在 prod，非 404）。补一轮 **经 prod 网关** 的 Owner 点击/API 证据即可收口。

## 范围

1. 未登录：401（可复用一句 JSON）。  
2. 已绑 OAuth 账号（建议仍 `ydowner_tang`）：经前端所连 **prod**（或直打 137）同步一次 → 200 或 429（**不能 404/500**）。  
3. 证据入 `docs/assets/t-fe-repo-sync-prod/`（JSON ± 截图）；注明 `DEPLOY_COMMIT` 若能读到。

## 非范围

- 不改功能（除非 prod 仍 500 因 body null——本机已修 `{}`）  
- 不等 A 再写长汇报（路由 401 已证明在）

## 验收

1. prod 路径非 404  
2. 证据 + 三句话（可无大 commit）

## 汇报格式

做了什么 / 证据 / 疑问。

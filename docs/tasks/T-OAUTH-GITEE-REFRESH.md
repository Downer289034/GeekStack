# T-OAUTH-GITEE-REFRESH · Gitee token 到期自动刷新

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P1 · T-REPO-1 follow-up  
> 派发：2026-09-20（T-ADMIN-FEATURE 关单后）

## 背景

`user_oauth_binding` 已存 `refresh_token` / `expires_at`，但 `RepoSyncService` 同步前不刷新。Gitee token 过期 → Provider 抛错 → 虽已保留旧快照，用户仍无法更新仓库列表。

## 范围

1. 同步前：若 provider=GITEE 且 `expires_at` 已过期（含短缓冲，如 60s），用 `refresh_token` 调 Gitee OAuth refresh，AES 写回 `access_token_enc`，更新 `expires_at`（及若返回新 refresh 则一并更新）。  
2. refresh 失败：打 WARN，抛出可识别异常，**不写空快照**（沿用 T-REPO-1 失败保留）。  
3. GitHub：若当前无 refresh 流程，本单可不做或仅文档注明。  
4. 单测：mock 过期 → refresh 成功再拉仓；refresh 失败保留旧快照（await 异步）。  
5. 密钥不进日志/git。

## 非范围

- 不做前端改动  
- 不改 Admin 加精/置顶  
- 不强制用户重新 OAuth（能静默 refresh 则静默）

## 验收

1. 过期 token 场景：同步仍能更新或明确失败且旧列表仍在  
2. 测例绿；独立 commit  
3. 三句话汇报

## 完成说明（开发 A，2026-09-20）

- `GiteeOAuthService.refreshAccessToken`；`RepoSyncService` 同步前 60s 缓冲续期并 AES 回写
- GitHub：本单不做 refresh（无既有流程）
- 测例：`expiredTokenRefreshSuccessUpdatesSnapshotAndRewritesToken`、`refreshFailurePreservesOldSnapshot`（await）

## 汇报格式

做了什么 / 证据 / 疑问。

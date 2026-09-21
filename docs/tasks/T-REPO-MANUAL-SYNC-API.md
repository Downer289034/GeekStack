# T-REPO-MANUAL-SYNC-API · 主页手动同步仓库 API

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P2 · T-REPO-2 遗留  
> 派发：2026-09-21（T-ADMIN-FEATURE-LIVE-SMOKE 关单后）

## 背景

OAuth 绑定后会 `syncRepos`；主页无「手动刷新」入口。T-REPO-2 明确不做 FE 按钮，因缺稳定公开 API。现补后端，前端可另单接。

## 范围

1. Owner 鉴权接口（建议）：`POST /api/v1/me/repos/sync` body/query `provider=GITHUB|GITEE`（或两个都同步，文档写清）。  
2. 调用现有 `RepoSyncService.syncRepos`（含 Gitee refresh）；失败保留旧快照，返回可识别错误码/文案（勿 500 空壳）。  
3. 未绑定对应 provider → 4xx + 明确 message。  
4. 限流：同用户短时防抖（如 30～60s，简单内存/Redis 均可，文档注明）。  
5. 集成测：成功路径 mock Provider；未绑定；限流（可选但推荐）。

## 非范围

- 不写前端按钮（B 另单）  
- 不改加精 API  
- 不改 seed

## 验收

1. 契约可读；测例绿；独立 commit  
2. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。

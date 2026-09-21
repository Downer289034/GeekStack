# T-FE-REPO-SYNC-BTN · 主页「同步仓库」按钮

> 负责人：**开发 B（极客栈前端）**  
> 优先级：P2 · T-REPO-2 遗留  
> 派发：2026-09-21（T-FE-FEATURE-PIN-BADGE 关单后）  
> 依赖：开发 A `docs/tasks/T-REPO-MANUAL-SYNC-API.md`（`POST /api/v1/me/repos/sync`）

## 背景

空态已拆；用户仍无法主动刷新仓库快照。后端正在补手动同步 API。

## 范围

1. Owner 主页「我的仓库」区：已绑定 OAuth 时显示「同步仓库」（或按 provider 分 GitHub/Gitee）。  
2. 调用 A 交付的 sync API；成功后刷新 `fetchRepos`；失败 toast 可读文案（含未绑定、限流）。  
3. 请求中按钮 loading / 防连点。  
4. 截图：绑定态可见按钮 +（可选）同步后列表变化，入 `docs/assets/t-fe-repo-sync-btn/`。

## 非范围

- 不实现后端 sync  
- 不改空态文案逻辑（T-REPO-2 已关）  
- 不做 Admin

## 协调

若 A 尚未合入：可先按任务书约定路径写 `api/repo.ts` + UI，用 MSW/临时 mock 自测；合入后改真接口再关单。汇报注明对接 commit。

## 验收

1. Owner 可点同步；非 owner 无按钮  
2. 证据截图 + 独立 commit  
3. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。

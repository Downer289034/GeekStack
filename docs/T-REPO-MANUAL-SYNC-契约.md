# T-REPO-MANUAL-SYNC 契约（主页手动同步仓库）

鉴权：`Authorization: Bearer <JWT>`（本人）。未登录 → HTTP 401 / `code=40100`。

## 手动同步

```
POST /api/v1/me/repos/sync?provider=GITHUB
# 或
POST /api/v1/me/repos/sync
Content-Type: application/json
{ "provider": "GITEE" }
```

- `provider`：`GITHUB` | `GITEE`（大小写不敏感）；query 优先于 body；缺省/非法 → HTTP 400 / `code=40000`
- 成功 → HTTP 200 / `code=0`，data：

```json
{ "provider": "GITHUB", "syncedCount": 3 }
```

- 未绑定该 provider → HTTP 404 / `code=40400`，message 含「未绑定 … 请先完成 OAuth 授权」
- 远端 API / Gitee refresh 失败 → HTTP 503 / `code=50300`，**旧快照保留**，message「同步失败，已保留上次仓库快照」
- 同用户同 provider 冷却期内重复调用 → HTTP 429 / `code=42900`

## 限流

- 默认冷却 **60 秒**（`community.oauth.repo-sync-cooldown-seconds` / env `REPO_SYNC_COOLDOWN_SECONDS`）
- 实现：**进程内内存**（`RepoSyncRateLimiter`）；多实例部署时各节点独立，不跨节点共享

## 行为

调用现有 `RepoSyncService`（含 Gitee token 临近过期时 refresh）。成功时物理替换该 provider 快照并保留 hidden 偏好；失败不写空快照。

## 非范围

前端「刷新」按钮（另派 B）；不改加精/置顶 API。

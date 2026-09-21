# T-FE-REPO-SYNC-PROD 证据

- 网关：`http://172.25.41.137/api/v1/me/repos/sync`
- `DEPLOY_COMMIT`：`c868d87`
- 未登录 → **401**（路由在，非 404）
- Owner 同步：先 **200** `syncedCount=2`，经 nginx 二次 → **429** 限流（验收接受）
- 说明：prod `geekstack-mysql` 原无 OAuth 绑定（绑定在 `community-mysql`）；冒烟用临时账号 + 短暂借调 Gitee binding，结束后已删除

| 场景 | 文件 | 结果 |
|---|---|---|
| 未登录 401 | 00-anon-sync.json | pass |
| 网关 Owner 同步 429 | 01-owner-sync-gitee.json | pass |
| 同 backend 200 | 01b-owner-sync-gitee-200.json | pass |


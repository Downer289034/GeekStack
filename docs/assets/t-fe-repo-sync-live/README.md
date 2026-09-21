# T-FE-REPO-SYNC-LIVE 证据

- API：现行 Vite 代理本机 `http://127.0.0.1:8080`（含 `POST /api/v1/me/repos/sync`）
- 账号：`ydowner_tang`（已绑 Gitee；口令未入库）
- 对接后端 commit：`2f63e92`（本地进程已重启加载）
- prod `DEPLOY_COMMIT` 当时仍为 `2543a78`（A 的 T-DEPLOY-BACKEND-SYNC-IMAGE 未改文件）；本机未登录 → 401 门禁已满足
- 真机小修：`syncMyRepos` 改传 JSON `{}`（`post(null)` 会被当成 form-urlencoded → 500）

| 场景 | 文件 | 结果 |
|---|---|---|
| 未登录 sync → 401 | 00-anon-sync.json | pass |
| API 直连成功 | 01-api-sync-gitee.json（http 200 / syncedCount=2） | pass |
| Owner 可见同步按钮 | 02-owner-sync-btn.png | pass |
| UI 点击同步非路由 404 | 03-after-click-sync.png + 03-ui-sync-response.json（本次 429 限流，可接受） | pass |

```json
[
  {
    "id": "anon-401",
    "status": "pass",
    "detail": "http=401; code=40100; msg=未登录或登录已过期"
  },
  {
    "id": "api-sync-not-404",
    "status": "pass",
    "detail": "http=200; code=0; msg=ok"
  },
  {
    "id": "ui-click-sync",
    "status": "pass",
    "detail": "http=429; code=42900; msg=同步过于频繁，请 57 秒后再试; data=null"
  }
]
```

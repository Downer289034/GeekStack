# T-FE-REPO-SYNC-BTN 证据

- 契约：`POST /api/v1/me/repos/sync?provider=GITHUB|GITEE`（依赖 A `T-REPO-MANUAL-SYNC-API`）
- 本证据为 Playwright mock（A 未合入 origin 时仍可关 UI 验收）

| 场景 | 截图 | 结果 |
|---|---|---|
| Owner 绑定态可见同步按钮 | 01-owner-bound-sync-btns.png | pass |
| 点击同步后刷新列表 | 02-owner-after-sync.png | pass |
| 访客无同步按钮 | 03-visitor-no-sync-btn.png | pass |

```json
[
  {
    "id": "owner-bound-btns",
    "status": "pass",
    "detail": "github=true; gitee=true"
  },
  {
    "id": "owner-sync-refresh",
    "status": "pass",
    "detail": "syncHits=1; repoVisible=1"
  },
  {
    "id": "visitor-no-btn",
    "status": "pass",
    "detail": "github=0; gitee=0"
  }
]
```

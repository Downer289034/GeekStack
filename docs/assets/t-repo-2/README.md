# T-REPO-2 主页仓库空态证据

| 场景 | 截图 | 结果 |
|---|---|---|
| owner 未绑定 → 去绑定 | 01-owner-unbound.png | pass |
| owner 已绑定无仓 | 02-owner-bound-empty.png | pass |
| repos 失败不整页 404 | 03-repos-fail-not-404.png | pass |

```json
[
  {
    "id": "owner-unbound",
    "status": "pass",
    "detail": "bindBtn=1; desc=1; onSettings=true"
  },
  {
    "id": "owner-bound-empty",
    "status": "pass",
    "detail": "neutral=1; bindCta=0; not404=true"
  },
  {
    "id": "repos-fail-not-404",
    "status": "pass",
    "detail": "profileOk=2; repoErr=1; is404=0"
  }
]
```

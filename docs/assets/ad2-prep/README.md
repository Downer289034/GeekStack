# AD2 联调日准备（极客栈侧推送冒烟）

> 2026-09-20｜开发 A｜工作区 `2026-09-06-18-49-57`

## 配置确认（无明文）

| 项 | 状态 |
|----|------|
| `AGENT_WEBHOOK_URL` | `http://172.25.41.137:18000/api/v1/events`（本机 `.env`） |
| `STEWARD_WEBHOOK_SECRET` | 已配置（len=64 hex）；与桌面 handoff 同源；**未入 git/汇报** |
| 本机 backend `:8081` | UP；已重启加载当前 `.env` |
| 管家 `:18000` | TCP 可达；`POST /api/v1/events` 有响应 |

## 冒烟结果（阻塞）

三类创建均已从本机 backend 发出，管家统一返回：

```text
HTTP 503  {"detail":{"error":"webhook_secret_not_configured"}}
```

| 事件 | contentId | status | trace_id |
|------|-----------|--------|----------|
| post.created | post_3114 | 503 | （未拿到；非 202） |
| comment.created | comment_1308 | 503 | （未拿到） |
| share.created | share_2351 | 503 | （未拿到） |

坏签名直连探针：同样 **503 `webhook_secret_not_configured`**（secret 未写入前验签路径不可达，无法验证 401 拒签）。

**阻塞方**：开发 C — 管家侧写入与 A 线下交接的同一 `STEWARD_WEBHOOK_SECRET` 后重跑。

## 解阻后复跑

```powershell
# 仓库根；勿打印 secret
Get-Content .env | % { if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }; $k,$v=$_.Split('=',2); Set-Item env:$($k.Trim()) $v.Trim() }
python scripts/ad2-webhook-smoke.py good   # 期望 202 + trace_id
python scripts/ad2-webhook-smoke.py bad    # 期望 401/拒签（非 503）
# 或走业务：注册用户 → 发帖/评论/分享，grep 后端日志「Webhook 推送成功」
```

证据 JSON：`smoke-result.json`。值守：`duty-checklist.md`。

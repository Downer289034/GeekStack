# AD2 联调日值守清单（5 行）

1. **开发 A（极客栈）**：确认本机/dev `.env` 的 `AGENT_WEBHOOK_URL` + `STEWARD_WEBHOOK_SECRET` 已加载（backend 重启后）；联调窗口内用发帖/评论/分享冒烟，日志搜 `Webhook 推送成功` 与 `agentTraceId`。
2. **开发 C（管家）**：确认管家 `:18000` 已起，且侧边 secret 与 A 线下交接值一致；验收 `POST /api/v1/events` 对正确签名回 **202 + trace_id**，坏签名拒签（401）。
3. **失败先看（极客栈）**：backend 控制台 / `WebhookPushListener` 日志 — `推送成功` vs `HTTP 错误`（401/422/503/超时）及 `contentId`；处置联调确认 `steward_bot` 仍为 ADMIN 可登录。
4. **失败先看（管家）**：管家 events 接入/验签日志 — `webhook_secret_not_configured`、签名失败、422 schema；对账字段 `trace_id`。
5. **升档条件**：连续两类事件拿不到 202 → 双方对表 secret 是否同源 + URL 是否 `:18000` + body 是否 snake_case + HMAC 是否 UTF-8（勿 hex decode）；勿在聊天贴明文 secret。

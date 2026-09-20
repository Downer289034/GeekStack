# 联调前置（HMAC + webhook + steward_bot）自测证据

> 2026-09-20｜开发 A｜工作区 `2026-09-06-18-49-57`｜密钥/密码未入库

## 1. HMAC
- 生成：32 字节 hex（`openssl` 不可用时用 CSPRNG 等价）
- 写入本机根目录 `.env` 的 `STEWARD_WEBHOOK_SECRET`（gitignore）
- **secret 已线下交接**（桌面 handoff 文件交开发 C；聊天/git 无明文）

## 2. Webhook
- `AGENT_WEBHOOK_URL=http://172.25.41.137:18000/api/v1/events`
- 探活：`:18000` TCP 可达；`OPTIONS /api/v1/events` → 405（端点在，方法不允许属预期）
- 说明：backend 需重启后才加载新 secret（与管家同值后联调）

## 3. steward_bot 三步
| 步骤 | 结果 |
|------|------|
| 注册 + promote-admin | `id=7133 role=ADMIN status=0` |
| 重新登录 | `me.role=ADMIN` |
| 删帖 | `DELETE /api/v1/admin/posts/3110` → code=0；审计 `operator=steward_bot` |
| 禁言 | `PUT /api/v1/admin/users/7135/status` status=1 → code=0；审计 `operator=steward_bot` |
| 封禁后登录 | target 被拒（403） |

审计摘录见 `admin-audit-excerpt.log`；API 摘要见 `selftest-summary.json`（无 token/密码）。

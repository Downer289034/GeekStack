# AD2 联调日执行交接（交开发 C 写入纪要）

> 2026-09-20 18:22｜开发 A｜无明文 secret / 无用户密码

## 环境对表

| 项 | 结果 |
|----|------|
| `AGENT_WEBHOOK_URL` | `http://172.25.41.137:18000/api/v1/events` |
| 探针 good / bad | 202 / 401 |
| `steward_bot` | id=7133 · ADMIN · status=0 |

## 三类事件（请写入纪要）

| 事件 | contentId | agentTraceId | localTrace |
|------|-----------|--------------|------------|
| post.created | **post_3117** | **wf-20260920-f415d8** | wf-d39d20801ea9 |
| comment.created | **comment_1310** | **wf-20260920-882f17** | wf-8e066ff5939f |
| share.created | **share_2353** | **wf-20260920-e95f85** | wf-5a832c591b8e |

日志均为 `Webhook 推送成功`（HTTP 202 路径）。

## 处置抽检（A 配合完成）

| 动作 | 目标 | 结果 | 审计 |
|------|------|------|------|
| 删帖 | post **3118** | code=0 | `ADMIN_DELETE_POST operator=steward_bot` |
| 禁言→解封 | user **7150** (`ad2tgt_3268`) | code=0 / code=0 | `ADMIN_SET_USER_STATUS operator=steward_bot` |

机器可读摘要：`handoff-to-C.json`。

# T-ADMIN-FEATURE-LIVE-SMOKE · steward_bot 加精/置顶真机证据

> 2026-09-21｜开发 A｜工作区 `2026-09-06-18-49-57`｜无 token / 无密码

## 环境

| 项 | 值 |
|----|----|
| API | `http://172.25.41.137`（prod nginx → `geekstack-backend`） |
| 账号 | `steward_bot`（凭据仅本机 OFFLINE；未入库） |
| 目标帖 | `postId=164` |

## 冒烟结果

| 动作 | 请求 | code |
|------|------|------|
| 加精 | `PUT /api/v1/admin/posts/164/featured` `{featured:1}` | **0** |
| 置顶 | `PUT /api/v1/admin/posts/164/pinned` `{pinned:1}` | **0** |
| 取消加精 | `{featured:0}` | **0** |
| 取消置顶 | `{pinned:0}` | **0** |

## 审计（operator=`steward_bot`）

摘录见 `admin-audit-excerpt.log`：

- `ADMIN_SET_POST_FEATURED` · `operatorId=628` · `featured=1` / `0`
- `ADMIN_SET_POST_PINNED` · `operatorId=628` · `pinned=1` / `0`

API 摘要见 `smoke-summary.json`。供管家 MA3 对照。

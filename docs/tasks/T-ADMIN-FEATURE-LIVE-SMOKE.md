# T-ADMIN-FEATURE-LIVE-SMOKE · steward_bot 加精/置顶真机冒烟

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P1 · 给管家 MA3 真处置留证据  
> 派发：2026-09-21（T-DEPLOY-BACKEND-IMAGE 关单后）  
> 契约：`docs/T-ADMIN-FEATURE-契约.md`

## 背景

镜像已含 Admin 加精/置顶路由（未登录 401 已验）。管家 digest 处置映射已齐，缺一份 **steward_bot 真写** 脱敏证据，便于 MA3 演示/联调对照。

## 范围

1. 用 `steward_bot`（凭据仅本地/OFFLINE，**勿进 git/汇报明文**）登录。  
2. 选一测试帖（可用 seed/`[REAL]` 或专用测试帖）：  
   - `PUT /api/v1/admin/posts/{id}/featured` `{featured:1}` → 再 `0` 还原（或说明保留加精）  
   - `PUT .../pinned` 同理至少走通一次  
3. 查审计：operator=`steward_bot`，动作可区分加精/置顶。  
4. 证据入 `docs/assets/t-admin-feature-live/`（命令摘要 JSON / 脱敏响应，无 token）。

## 非范围

- 不改业务代码（除非冒烟发现必须修的小 bug）  
- 不改管家仓  
- 不强制前端运营 UI

## 验收

1. featured / pinned 各至少一次成功（code=0）  
2. 审计可指向 steward_bot；证据脱敏  
3. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。

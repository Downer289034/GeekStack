# T-DATA-1 一期草案（可联调口径）

> 2026-09-20｜开发 A｜面向管家 R2 调查工具数据源  
> 基线：清单 T-DATA-1 + `rag-reuse-position.md` §五 + 契约 v2 delta §四（数据面仍为预告）

## 1. 本单交付

| 端点 | 说明 |
|------|------|
| `GET /api/v1/admin/data/users/{userKey}/posts` | 用户发帖历史（分页，脱敏 ID） |
| `GET /api/v1/admin/data/users/{userKey}/comments` | 用户评论历史（分页，脱敏 ID） |
| `GET /api/v1/admin/data/stats` | 社区聚合统计 |
| `GET /api/v1/admin/data/penalties` | **判罚回流占位**：空列表 + `placeholder=true` |

鉴权：与 T-M4-1 一致，`/api/v1/admin/**` → `hasRole('ADMIN')`（`steward_bot` 服务账号）。  
三路径：ADMIN 成功 / 未认证 401 / 普通 USER 403。

## 2. 脱敏口径（一期钉死）

- ID：`user_{id}` / `post_{id}` / `comment_{id}` / `share_{id}`（与事件契约前缀一致）
- 可返回：username、title、正文截断（≤500）、createdAt、deleted 标记
- **禁止**：email、passwordHash、JWT/refresh、OAuth token、AES 密文、tokenVersion

## 3. 已拍板假设（有异议再改）

1. **不做独立 STEWARD 角色**：一期复用 ADMIN；后续若要机器角色再增量。
2. **`userKey`**：纯数字或 `user_{id}`；用户不存在 → 404。
3. **逻辑删除内容**：一期只返回未删记录（MP 全局过滤）；含删记录待契约评审。
4. **判罚回流**：无表无写入；端点空实现，`penaltyCount=0`，文档标明待 v2。

## 4. 待双方确认（不阻塞一期）

- Q1：明细是否需含 Skill / Share 历史（本单仅 post+comment）？
- Q2：正文截断长度是否改为事件口径 5000？
- Q3：判罚回流字段表（action / reason / operator / contentId）定稿后替换占位。

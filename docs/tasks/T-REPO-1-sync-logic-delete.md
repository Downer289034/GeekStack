# T-REPO-1 · 仓库同步逻辑删除导致主页永久空（P0）

**负责人：后端 A**｜**优先级：P0**｜**派发：2026-09-20**

## 现象（证据）

- 主页「我的仓库」显示「未绑定仓库或同步失败」
- 账号 `@ydowner_tang`：设置里已绑 Gitee，DB 有 2 条 `user_repo`，但 `deleted=1`
- `GET /api/v1/users/ydowner_tang/repos` → `data:[]`（MP 全局逻辑删过滤）

## 根因

`RepoSyncService.syncSnapshot`：

1. `repoMapper.delete(...)` → MyBatis-Plus **逻辑删除**（`deleted=1`），行仍占唯一键 `(user_id, provider, repo_full_name)`
2. 随后 `insert` 撞唯一键 → 捕获 `DuplicateKeyException` 后 **跳过**
3. 列表只查 `deleted=0` → **永久空列表**

叠加：`GitHubRepoProvider` / `GiteeRepoProvider` 失败时 `return List.of()`，会被当成「成功 0 仓」走同一条 syncSnapshot，把旧快照逻辑删光。

## 设计口径（应恢复）

1. 必须先有 OAuth 绑定才同步
2. 只同步公开仓
3. **平台 API 失败 → 保留旧快照（物理保留 / 不写库）**
4. 成功同步 → 全量替换快照，且新行 `deleted=0` 可见
5. 重同步保留 `hidden` 偏好

## 修复要求

1. `syncSnapshot` 对旧快照做**物理 DELETE**（或等价：先按唯一键 upsert 并把 `deleted` 置 0，再删远端已不存在的行）；禁止再用会触发逻辑删的 `mapper.delete` 做「清空再插入」
2. Provider 失败必须**抛异常**（或返回失败结果），禁止吞异常返回空列表；上层 catch 后不写库
3. 补/修集成测试：
   - 成功同步后列表可见
   - **二次同步**后列表仍可见（回归本 bug）
   - API 失败时旧快照仍在（await `@Async`）
4. （同任务或 follow-up）Gitee：`expires_at` 到期用 `refresh_token` 刷新；本 bug 修完再开亦可，但需在报告注明

## 验收

- 本地复现：对已有 `user_repo` 用户再触发一次 OAuth/sync，主页仍能列出仓库
- `ydowner_tang`（或等价夹具）二次 sync 后 `GET .../repos` 非空
- 相关单测绿；提交独立 commit

## 临时数据 hotfix（运维已可跑）

`deploy/dev/hotfix-user-repo-logic-delete.sql`：把误逻辑删的行 `deleted=0` 拉回（不替代代码修复）。

## 前端（B，另单）

空态文案拆分 + owner「去绑定」——见 T-REPO-2，本单不掺。

## 完成说明（开发 A，2026-09-20）

- `UserRepoMapper.physicalDeleteByUserAndProvider` + `syncSnapshot` 改物理 DELETE
- GitHub/Gitee Provider 失败抛 `IllegalStateException`，禁止吞成空列表
- 回归：`RepoSyncSnapshotTest`（二次 sync / ghost 复活 / hidden 保留）+ `RepoSyncServiceTest` 失败路径 await
- **未做（follow-up）**：Gitee `expires_at` 到期用 `refresh_token` 刷新

# T-ADMIN-FEATURE · 加精/置顶 Admin API（P3 MA3 依赖）

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P1 · P3 对外时钟  
> 派发：2026-09-20（T-REPO-1 关单后）  
> 依据：管家 `docs/p3-charter.md` MA3 依赖

## 背景

管家 P3 MA3（精华聚合）需要把「加精/置顶建议」落到极客栈。当前 `AdminController` 仅有删帖、用户封禁。

## 范围

1. Admin 鉴权（复用 ADMIN / `steward_bot`）下提供：
   - 帖子加精 / 取消加精（字段以现有 `post` 表或增量迁移为准；若无字段则 Flyway 增量加 `is_featured` 或等价）
   - 帖子置顶 / 取消置顶（已有 `is_pinned` 则复用）
2. 审计：operator 可区分（含 `steward_bot`）
3. 集成测试：成功 / 401 / 403；幂等（重复加精不炸）
4. 契约短文：路径、body、错误码，供管家 AD1 映射（可放 `docs/T-ADMIN-FEATURE-契约.md`）

## 非范围

- 不做推荐位算法、不做前端运营台大改
- 不做 Gitee refresh（另 follow-up）
- 不混入未提交的镜像纪律文档，除非本单明确需要

## 完成说明（开发 A，2026-09-20）

- Flyway `V8__post_add_is_featured.sql`；`PUT .../featured`、`PUT .../pinned`
- 审计 `ADMIN_SET_POST_FEATURED` / `ADMIN_SET_POST_PINNED`（含 operator）
- 测例：`adminFeatureAndPinSucceedAndReflectOnDetail`、`userFeatureForbiddenAndAnonymousUnauthorized`、`featureMissingPostReturns404`
- 契约：`docs/T-ADMIN-FEATURE-契约.md`

## 验收

1. 端点可被 ADMIN JWT 调用，普通用户 403  
2. 列表/详情能反映加精/置顶状态（至少帖子详情或管理查询可见）  
3. 测试绿；独立 commit；汇报含路径与测例名  
4. 密钥不进 git

## 汇报

三句话：做了什么 / 证据 / 疑问。

-- =====================================================================
-- 一次性提权：将指定用户提升为 ADMIN（T-M4-1）
-- 运维手动执行，不入 Flyway 自动迁移。
--
-- 用法：
--   1. 先用普通注册或已有账号确认 username
--   2. 将下方 REPLACE_WITH_ADMIN_USERNAME 换成真实用户名后执行
--   3. 目标用户必须重新登录（或走 /auth/refresh 换发新令牌）
--
-- 【预期行为，勿当 bug】
--   权限来自 JWT 的 role claim，不是每次请求查库。
--   提权只改 DB；旧 access token 无 role / 仍为 USER → /api/v1/admin/** 会 403。
--   重新登录或 refresh 后新令牌带 role=ADMIN 即可。
-- =====================================================================

-- 示例（请改成真实用户名）：
-- UPDATE `user` SET role = 'ADMIN' WHERE username = 'your_admin_username' AND deleted = 0;

UPDATE `user`
SET role = 'ADMIN'
WHERE username = 'REPLACE_WITH_ADMIN_USERNAME'
  AND deleted = 0;

-- 执行后可用下列语句确认（库侧已是 ADMIN；生效仍须换发令牌）：
-- SELECT id, username, role, status FROM `user` WHERE username = 'REPLACE_WITH_ADMIN_USERNAME';

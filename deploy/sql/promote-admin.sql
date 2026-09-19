-- =====================================================================
-- 一次性提权：将指定用户提升为 ADMIN（T-M4-1）
-- 运维手动执行，不入 Flyway 自动迁移。
--
-- 用法：
--   1. 先用普通注册或已有账号确认 username
--   2. 将下方 :username 替换为真实用户名后执行
--   3. 目标用户须重新登录（refresh），JWT 才会带上 role=ADMIN
-- =====================================================================

-- 示例（请改成真实用户名）：
-- UPDATE `user` SET role = 'ADMIN' WHERE username = 'your_admin_username' AND deleted = 0;

UPDATE `user`
SET role = 'ADMIN'
WHERE username = 'REPLACE_WITH_ADMIN_USERNAME'
  AND deleted = 0;

-- 执行后可用下列语句确认：
-- SELECT id, username, role, status FROM `user` WHERE username = 'REPLACE_WITH_ADMIN_USERNAME';

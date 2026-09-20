-- 紧急数据修复：user_repo 逻辑删除残骸导致主页仓库永久空
-- 根因：RepoSyncService.syncSnapshot 用 MP delete(=逻辑删) + insert，唯一键撞 deleted=1 行 → 插入跳过
UPDATE user_repo SET deleted = 0, updated_at = NOW() WHERE deleted = 1;
SELECT COUNT(*) AS restored FROM user_repo WHERE deleted = 0;
SELECT u.username, r.repo_full_name, r.deleted, r.hidden
FROM user_repo r JOIN `user` u ON u.id=r.user_id
WHERE u.username='ydowner_tang';

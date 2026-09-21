-- 清除 demo_* 占位假数据（保留 real_* / 其它业务账号）
-- T-SEED-1：边界 = username LIKE 'demo\_%' ESCAPE '\' + 标题 LIKE '[DEMO]%'
-- 禁止改写为清 real_* 或全表 truncate
SET NAMES utf8mb4;
DELETE ul FROM user_like ul
 INNER JOIN `user` u ON u.id = ul.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN post p ON ul.target_type = 'POST' AND ul.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN project_share s ON ul.target_type = 'SHARE' AND ul.target_id = s.id
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN skill sk ON ul.target_type = 'SKILL' AND ul.target_id = sk.id
 INNER JOIN `user` u ON u.id = sk.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN comment c ON ul.target_type = 'COMMENT' AND ul.target_id = c.id
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE uf FROM user_favorite uf
 INNER JOIN `user` u ON u.id = uf.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE uf FROM user_favorite uf
 INNER JOIN post p ON uf.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE uf FROM user_favorite uf
 INNER JOIN project_share s ON uf.target_id = s.id
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE n FROM notification n
 INNER JOIN `user` u ON u.id = n.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE c FROM comment c
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\' AND c.parent_id IS NOT NULL;

DELETE c FROM comment c
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE c FROM comment c
 INNER JOIN post p ON c.target_type = 'POST' AND c.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE c FROM comment c
 INNER JOIN project_share s ON c.target_type = 'SHARE' AND c.target_id = s.id
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE sk FROM skill sk
 INNER JOIN `user` u ON u.id = sk.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE p FROM post p
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE s FROM project_share s
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE r FROM user_repo r
 INNER JOIN `user` u ON u.id = r.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE b FROM user_oauth_binding b
 INNER JOIN `user` u ON u.id = b.user_id
 WHERE u.username LIKE 'demo\\_%' ESCAPE '\\';

DELETE FROM `user` WHERE username LIKE 'demo\\_%' ESCAPE '\\';

-- 兜底：标题仍带 [DEMO] 的残留（若作者非 demo_*）
DELETE ul FROM user_like ul
 INNER JOIN post p ON ul.target_type = 'POST' AND ul.target_id = p.id
 WHERE p.title LIKE '[DEMO]%';
DELETE ul FROM user_like ul
 INNER JOIN project_share s ON ul.target_type = 'SHARE' AND ul.target_id = s.id
 WHERE s.title LIKE '[DEMO]%';
DELETE c FROM comment c
 INNER JOIN post p ON c.target_type = 'POST' AND c.target_id = p.id
 WHERE p.title LIKE '[DEMO]%' AND c.parent_id IS NOT NULL;
DELETE c FROM comment c
 INNER JOIN post p ON c.target_type = 'POST' AND c.target_id = p.id
 WHERE p.title LIKE '[DEMO]%';
DELETE c FROM comment c
 INNER JOIN project_share s ON c.target_type = 'SHARE' AND c.target_id = s.id
 WHERE s.title LIKE '[DEMO]%' AND c.parent_id IS NOT NULL;
DELETE c FROM comment c
 INNER JOIN project_share s ON c.target_type = 'SHARE' AND c.target_id = s.id
 WHERE s.title LIKE '[DEMO]%';
DELETE FROM post WHERE title LIKE '[DEMO]%';
DELETE FROM project_share WHERE title LIKE '[DEMO]%';

SELECT 'after_purge' AS msg,
  (SELECT COUNT(*) FROM `user` WHERE username LIKE 'demo\\_%' ESCAPE '\\') AS demo_users,
  (SELECT COUNT(*) FROM `user` WHERE username LIKE 'real\\_%' ESCAPE '\\') AS real_users,
  (SELECT COUNT(*) FROM post WHERE title LIKE '[DEMO]%') AS demo_posts,
  (SELECT COUNT(*) FROM post WHERE title LIKE '[REAL]%') AS real_posts,
  (SELECT COUNT(*) FROM project_share WHERE title LIKE '[DEMO]%') AS demo_shares,
  (SELECT COUNT(*) FROM project_share s JOIN `user` u ON u.id=s.user_id WHERE u.username LIKE 'real\\_%' ESCAPE '\\') AS real_shares,
  (SELECT COUNT(*) FROM skill sk JOIN `user` u ON u.id=sk.user_id WHERE u.username LIKE 'real\\_%' ESCAPE '\\') AS real_skills;

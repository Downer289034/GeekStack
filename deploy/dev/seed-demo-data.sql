-- =====================================================================
-- T-BE-3 演示数据集（幂等）：重跑先清 demo_* 再插入，不产生脏数据
-- 账号：demo_alice / demo_bob / demo_carol ，密码均为 password123
-- 规模：3 用户 / 10 帖（含 5000+ 字、置顶）/ 8 分享（多标签）/ 15 评论（含楼中楼）
--       + 点赞明细 + view/reply/like 计数
-- =====================================================================

SET NAMES utf8mb4;
SET @demo_pw := '$2a$10$Uyvz95pTPx/qOjTmn7vwXeI.lFVGMTtwCaN4qkGTcU.1jjSYmw3yG';

-- ---------- 1) 清理（FK 安全顺序；仅 demo_*）----------
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

-- ---------- 2) 用户（3）----------
INSERT INTO `user` (username, email, password_hash, nickname, bio, tech_stack, role, status)
VALUES
('demo_alice', 'demo_alice@geekstack.local', @demo_pw, 'Alice·栈主',
 '极客栈演示账号·前端视觉与聚合验收用', JSON_ARRAY('Java', 'Spring', 'Vue'), 'USER', 0),
('demo_bob', 'demo_bob@geekstack.local', @demo_pw, 'Bob·回帖狂',
 '爱写长评与楼中楼', JSON_ARRAY('MySQL', 'Redis', 'AI'), 'USER', 0),
('demo_carol', 'demo_carol@geekstack.local', @demo_pw, 'Carol·分享控',
 '项目广场多标签样例作者', JSON_ARRAY('Docker', 'TypeScript', 'NaiveUI'), 'USER', 0);

SET @alice := (SELECT id FROM `user` WHERE username = 'demo_alice');
SET @bob   := (SELECT id FROM `user` WHERE username = 'demo_bob');
SET @carol := (SELECT id FROM `user` WHERE username = 'demo_carol');
SET @cat_java := (SELECT id FROM post_category WHERE slug = 'java');
SET @cat_ai   := (SELECT id FROM post_category WHERE slug = 'ai');
SET @cat_qa   := (SELECT id FROM post_category WHERE slug = 'qa');
SET @cat_res  := (SELECT id FROM post_category WHERE slug = 'resource');

-- ---------- 3) 帖子（10：置顶 + 长文 5000+ + 多分类多热度）----------
INSERT INTO post (user_id, category_id, title, content_md, view_count, reply_count, like_count, is_pinned, is_hot)
VALUES
(@alice, @cat_java, '[DEMO] 置顶：极客栈开发环境双栈说明',
 CONCAT('## 为什么要双栈\n\n生产栈不暴露 3306，开发栈暴露给本机。\n\n',
        REPEAT('这是演示长文填充段落，用于详情页滚动与 Markdown 渲染压测。', 180)),
 1200, 5, 8, 1, 1),
(@alice, @cat_java, '[DEMO] Spring Boot 3 与虚拟线程笔记',
 '短笔记：JDK 17 起步，虚拟线程可观察但不必急用。', 860, 3, 5, 0, 0),
(@bob, @cat_ai, '[DEMO] 本地 RAG 小实验记录',
 '向量库 + 小模型，验收首页「最新帖子」卡片。', 640, 2, 3, 0, 0),
(@carol, @cat_qa, '[DEMO] WCAG 对比度怎么量？',
 '问：白底主色如何保证 4.5:1？答：用对比度脚本实测。', 520, 2, 2, 0, 0),
(@alice, @cat_res, '[DEMO] 开源图标与封面占位清单',
 '资源帖：给 T-DESIGN 封面占位与空态用。', 410, 1, 1, 0, 0),
(@bob, @cat_java, '[DEMO] @Async 与通知落库时序',
 'F5 教训：异步断言必须轮询。Webhook 超时会饿死线程池。', 980, 2, 4, 0, 0),
(@carol, @cat_ai, '[DEMO] 标签色语义：紫青橙怎么分',
 '前端语义 token 验收素材。', 300, 0, 1, 0, 0),
(@alice, @cat_qa, '[DEMO] Flyway 已执行脚本能改吗？',
 '答：禁止回改，只允许 V4+ 增量。', 220, 0, 0, 0, 0),
(@bob, @cat_res, '[DEMO] 热榜徽标 1/2/3 视觉验收帖',
 '配合 /api/v1/hot 看 gs-hot-rank。', 1500, 0, 6, 0, 1),
(@carol, @cat_java, '[DEMO] MyBatis-Plus 3.5.12 断代备忘',
 'IGNORED→ALWAYS；分页构件拆分；禁止双重 LIMIT。', 180, 0, 0, 0, 0);

SET @p1 := (SELECT id FROM post WHERE title = '[DEMO] 置顶：极客栈开发环境双栈说明');
SET @p2 := (SELECT id FROM post WHERE title = '[DEMO] Spring Boot 3 与虚拟线程笔记');
SET @p3 := (SELECT id FROM post WHERE title = '[DEMO] 本地 RAG 小实验记录');
SET @p4 := (SELECT id FROM post WHERE title = '[DEMO] WCAG 对比度怎么量？');
SET @p5 := (SELECT id FROM post WHERE title = '[DEMO] 开源图标与封面占位清单');
SET @p6 := (SELECT id FROM post WHERE title = '[DEMO] @Async 与通知落库时序');
SET @p9 := (SELECT id FROM post WHERE title = '[DEMO] 热榜徽标 1/2/3 视觉验收帖');

-- 校正长文长度（硬条件：≥5000 字符）
UPDATE post
   SET content_md = CONCAT(
           content_md, '\n\n',
           REPEAT('补充段落·用于保证 CHAR_LENGTH≥5000。', 80))
 WHERE id = @p1 AND CHAR_LENGTH(content_md) < 5000;

-- ---------- 4) 项目分享（8：多标签，含封面占位 URL）----------
INSERT INTO project_share
  (user_id, title, summary, content_md, cover_url, repo_url, demo_url, tags, view_count, like_count, status)
VALUES
(@carol, '[DEMO] GeekStack 广场卡片样例 A', '多标签：Java + Spring + MySQL',
 '## 样例 A\n\n用于青色语义标签与封面占位。',
 'https://picsum.photos/seed/geekstack-a/800/450',
 'https://github.com/example/geekstack-a', NULL,
 JSON_ARRAY('Java', 'Spring', 'MySQL'), 420, 4, 'PUBLISHED'),
(@carol, '[DEMO] Vue3 主题 Token 实验室', '多标签：Vue + TypeScript + NaiveUI',
 '视觉改造对照项目。',
 'https://picsum.photos/seed/geekstack-b/800/450',
 'https://github.com/example/theme-lab', 'https://example.com/theme-lab',
 JSON_ARRAY('Vue', 'TypeScript', 'NaiveUI'), 380, 3, 'PUBLISHED'),
(@alice, '[DEMO] Redis 热榜实验床', '多标签：Redis + Java',
 'ZSet 热榜与 60s 缓存演示。',
 NULL, 'https://github.com/example/hot-zset', NULL,
 JSON_ARRAY('Redis', 'Java'), 510, 5, 'PUBLISHED'),
(@bob, '[DEMO] 本地向量检索玩具', '多标签：AI + Python + Docker',
 'RAG 玩具项目。',
 'https://picsum.photos/seed/geekstack-rag/800/450',
 'https://github.com/example/tiny-rag', NULL,
 JSON_ARRAY('AI', 'Python', 'Docker'), 290, 2, 'PUBLISHED'),
(@carol, '[DEMO] 对比度巡检脚本', '多标签：TypeScript + 无障碍',
 'T-DESIGN 对比度脚本配套。',
 NULL, 'https://github.com/example/contrast-check', NULL,
 JSON_ARRAY('TypeScript', 'A11y'), 210, 1, 'PUBLISHED'),
(@alice, '[DEMO] Flyway 迁移演练仓', '多标签：MySQL + Docker',
 'V1–V3 重建演练。',
 'https://picsum.photos/seed/geekstack-flyway/800/450',
 'https://github.com/example/flyway-drill', NULL,
 JSON_ARRAY('MySQL', 'Docker', 'Java'), 160, 1, 'PUBLISHED'),
(@bob, '[DEMO] OAuth 联调沙箱', '多标签：Spring + OAuth',
 'GitHub/Gitee 双 Client 沙箱说明。',
 NULL, 'https://github.com/example/oauth-sandbox', NULL,
 JSON_ARRAY('Spring', 'OAuth', 'Java'), 330, 2, 'PUBLISHED'),
(@carol, '[DEMO] Markdown XSS 靶场（安全）', '多标签：Vue + 安全',
 '仅存安全渲染示例，勿关 sanitize。',
 'https://picsum.photos/seed/geekstack-md/800/450',
 'https://github.com/example/md-xss-lab', NULL,
 JSON_ARRAY('Vue', 'Security', 'Markdown'), 140, 0, 'PUBLISHED');

SET @s1 := (SELECT id FROM project_share WHERE title = '[DEMO] GeekStack 广场卡片样例 A');
SET @s3 := (SELECT id FROM project_share WHERE title = '[DEMO] Redis 热榜实验床');

-- ---------- 5) 评论 15（含楼中楼）----------
-- 顶层 10 + 楼中楼 5 = 15
INSERT INTO comment (target_type, target_id, user_id, parent_id, content_md, like_count)
VALUES
('POST', @p1, @bob,   NULL, '顶层：双栈说明很关键，已按文档拉起 community-dev。', 2),
('POST', @p1, @carol, NULL, '顶层：置顶帖适合 Hero 下方热榜联调。', 1),
('POST', @p2, @bob,   NULL, '顶层：虚拟线程先观望同意。', 0),
('POST', @p2, @carol, NULL, '顶层：JDK 17 LTS 够用。', 1),
('POST', @p3, @alice, NULL, '顶层：RAG 笔记求扩展阅读。', 0),
('POST', @p4, @alice, NULL, '顶层：对比度脚本链接要不要置顶？', 0),
('POST', @p5, @bob,   NULL, '顶层：封面占位 picsum 可用。', 0),
('POST', @p6, @carol, NULL, '顶层：Webhook 超时修复后通知稳了。', 1),
('SHARE', @s1, @alice, NULL, '顶层：卡片标签色青紫橙都出来了。', 1),
('SHARE', @s3, @bob,   NULL, '顶层：热榜实验床 stars 不错。', 0);

SET @c1 := (SELECT id FROM comment WHERE content_md LIKE '顶层：双栈说明%' LIMIT 1);
SET @c2 := (SELECT id FROM comment WHERE content_md LIKE '顶层：置顶帖适合%' LIMIT 1);
SET @c3 := (SELECT id FROM comment WHERE content_md LIKE '顶层：虚拟线程%' LIMIT 1);
SET @c8 := (SELECT id FROM comment WHERE content_md LIKE '顶层：Webhook%' LIMIT 1);
SET @c9 := (SELECT id FROM comment WHERE content_md LIKE '顶层：卡片标签%' LIMIT 1);

INSERT INTO comment (target_type, target_id, user_id, parent_id, content_md, like_count)
VALUES
('POST', @p1, @alice, @c1, '楼中楼：感谢 Bob，dev 栈 3306 通了。', 1),
('POST', @p1, @bob,   @c2, '楼中楼：热榜徽标也一起看。', 0),
('POST', @p2, @alice, @c3, '楼中楼：补充一篇压测备忘。', 0),
('POST', @p6, @alice, @c8, '楼中楼：RestTemplate connect 2s 是关键。', 2),
('SHARE', @s1, @carol, @c9, '楼中楼：封面占位与空态下一轮打磨。', 0);

-- 回帖计数对齐（仅 demo 帖，避免碰测试数据）
UPDATE post p
   SET reply_count = (
       SELECT COUNT(*) FROM comment c
        WHERE c.target_type = 'POST' AND c.target_id = p.id AND c.deleted = 0),
       last_reply_at = (
       SELECT MAX(created_at) FROM comment c
        WHERE c.target_type = 'POST' AND c.target_id = p.id AND c.deleted = 0)
 WHERE p.user_id IN (@alice, @bob, @carol);

-- ---------- 6) 点赞（明细 + 计数）----------
INSERT INTO user_like (user_id, target_type, target_id)
VALUES
(@bob,   'POST', @p1),
(@carol, 'POST', @p1),
(@alice, 'POST', @p9),
(@bob,   'POST', @p9),
(@carol, 'POST', @p9),
(@bob,   'POST', @p2),
(@carol, 'POST', @p6),
(@alice, 'SHARE', @s1),
(@bob,   'SHARE', @s1),
(@carol, 'SHARE', @s3),
(@alice, 'COMMENT', @c1),
(@bob,   'COMMENT', @c8);

UPDATE post p
   SET like_count = (
       SELECT COUNT(*) FROM user_like l
        WHERE l.target_type = 'POST' AND l.target_id = p.id AND l.deleted = 0)
 WHERE p.user_id IN (@alice, @bob, @carol);

UPDATE project_share s
   SET like_count = (
       SELECT COUNT(*) FROM user_like l
        WHERE l.target_type = 'SHARE' AND l.target_id = s.id AND l.deleted = 0)
 WHERE s.user_id IN (@alice, @bob, @carol);

UPDATE comment c
   SET like_count = (
       SELECT COUNT(*) FROM user_like l
        WHERE l.target_type = 'COMMENT' AND l.target_id = c.id AND l.deleted = 0)
 WHERE c.user_id IN (@alice, @bob, @carol);

-- ---------- 7) 验收计数（脚本末尾打印）----------
SELECT 'demo_users' AS k, COUNT(*) AS v FROM `user` WHERE username LIKE 'demo\\_%' ESCAPE '\\'
UNION ALL
SELECT 'demo_posts', COUNT(*) FROM post WHERE user_id IN (@alice, @bob, @carol)
UNION ALL
SELECT 'demo_shares', COUNT(*) FROM project_share WHERE user_id IN (@alice, @bob, @carol)
UNION ALL
SELECT 'demo_comments', COUNT(*) FROM comment WHERE user_id IN (@alice, @bob, @carol)
   OR target_id IN (SELECT id FROM post WHERE user_id IN (@alice, @bob, @carol))
   OR (target_type = 'SHARE' AND target_id IN (SELECT id FROM project_share WHERE user_id IN (@alice, @bob, @carol)))
UNION ALL
SELECT 'demo_likes', COUNT(*) FROM user_like WHERE user_id IN (@alice, @bob, @carol)
UNION ALL
SELECT 'long_post_chars', CHAR_LENGTH(content_md) FROM post WHERE id = @p1;

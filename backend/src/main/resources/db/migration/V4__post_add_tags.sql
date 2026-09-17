-- T-TAG-BE：帖子标签（与 project_share.tags 同构 JSON 数组）
-- 幂等：已存在则跳过（双栈/重复执行安全）
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'post' AND column_name = 'tags'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `post` ADD COLUMN `tags` JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT ''帖子标签数组'' AFTER `content_md`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- T-SKILL-1：Skill 专区表 + user_like.target_type 扩展 SKILL（幂等）

CREATE TABLE IF NOT EXISTS `skill` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `summary` VARCHAR(500) NOT NULL,
  `content_md` MEDIUMTEXT NOT NULL COMMENT '提示词正文 Markdown',
  `model_adaptation_md` VARCHAR(1000) NULL COMMENT '模型适配说明',
  `tags` JSON NULL COMMENT '标签数组',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` ENUM('PUBLISHED','HIDDEN') NOT NULL DEFAULT 'PUBLISHED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_skill_status_created` (`status`, `created_at`),
  KEY `idx_skill_user` (`user_id`),
  CONSTRAINT `fk_skill_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @has_skill := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user_like'
    AND column_name = 'target_type'
    AND column_type LIKE '%''SKILL''%'
);
SET @sql := IF(
  @has_skill = 0,
  'ALTER TABLE `user_like` MODIFY COLUMN `target_type` ENUM(''POST'',''SHARE'',''COMMENT'',''SKILL'') NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

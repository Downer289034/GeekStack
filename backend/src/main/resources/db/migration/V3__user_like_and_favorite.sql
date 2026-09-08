-- V3：点赞明细表 user_like + 收藏明细表 user_favorite（T19）
-- target_type 带 CHECK 约束对齐 V1 枚举纪律

CREATE TABLE `user_like` (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED  NOT NULL,
    target_type ENUM ('POST', 'SHARE', 'COMMENT') NOT NULL,
    target_id   BIGINT UNSIGNED  NOT NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_like_user_target (user_id, target_type, target_id),
    KEY idx_like_target (target_type, target_id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '点赞明细';

CREATE TABLE `user_favorite` (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED  NOT NULL,
    target_id   BIGINT UNSIGNED  NOT NULL COMMENT '帖子 ID（仅 POST）',
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fav_user_target (user_id, target_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏明细（仅帖子）';

-- 给 post/share/comment 表补充 like_count（V1 未建此字段）
-- 使用信息模式检查，兼容所有 MySQL 8.0.x 版本
SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='post' AND column_name='like_count');
SET @sql := IF(@exists = 0, 'ALTER TABLE `post` ADD COLUMN `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''点赞数'' AFTER `is_hot`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='project_share' AND column_name='like_count');
SET @sql := IF(@exists = 0, 'ALTER TABLE `project_share` ADD COLUMN `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''点赞数'' AFTER `status`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='comment' AND column_name='like_count');
SET @sql := IF(@exists = 0, 'ALTER TABLE `comment` ADD COLUMN `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''点赞数'' AFTER `content_md`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

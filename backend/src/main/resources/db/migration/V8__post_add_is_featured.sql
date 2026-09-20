-- T-ADMIN-FEATURE：帖子加精标记（置顶复用已有 is_pinned）
ALTER TABLE `post`
    ADD COLUMN `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '加精：0 否 1 是' AFTER `is_pinned`;

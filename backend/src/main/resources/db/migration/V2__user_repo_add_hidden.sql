-- V2：user_repo 加 hidden 列（T17 手动隐藏仓库）
-- 全量更新默认值为 0（visible），已有数据不受影响
ALTER TABLE `user_repo` ADD COLUMN `hidden` TINYINT NOT NULL DEFAULT 0 COMMENT '隐藏：0 显示 1 隐藏' AFTER `synced_at`;

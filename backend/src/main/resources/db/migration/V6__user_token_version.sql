-- T-PROFILE-BE：改密/设密后使旧 refresh 失效
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'token_version'
);
SET @sql := IF(
  @exists = 0,
  'ALTER TABLE `user` ADD COLUMN `token_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''JWT 版本，改密时递增'' AFTER `status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

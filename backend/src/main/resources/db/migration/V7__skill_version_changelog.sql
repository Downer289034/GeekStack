-- T-SKILL-3-BE：skill.version / skill.changelog（存量回填 1.0.0）
SET @exists_ver := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'skill' AND column_name = 'version'
);
SET @sql_ver := IF(
  @exists_ver = 0,
  'ALTER TABLE `skill` ADD COLUMN `version` VARCHAR(32) NOT NULL DEFAULT ''1.0.0'' COMMENT ''SemVer 版本号'' AFTER `model_adaptation_md`',
  'SELECT 1'
);
PREPARE stmt_ver FROM @sql_ver;
EXECUTE stmt_ver;
DEALLOCATE PREPARE stmt_ver;

SET @exists_cl := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'skill' AND column_name = 'changelog'
);
SET @sql_cl := IF(
  @exists_cl = 0,
  'ALTER TABLE `skill` ADD COLUMN `changelog` VARCHAR(2000) NULL COMMENT ''版本更新说明'' AFTER `version`',
  'SELECT 1'
);
PREPARE stmt_cl FROM @sql_cl;
EXECUTE stmt_cl;
DEALLOCATE PREPARE stmt_cl;

UPDATE `skill` SET `version` = '1.0.0' WHERE `version` IS NULL OR `version` = '';

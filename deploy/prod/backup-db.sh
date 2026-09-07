#!/usr/bin/env bash
# MySQL 每日备份：mysqldump 全量 → 本地目录轮转保留 7 份
# 服务器 crontab（setup 时自动安装）：0 3 * * * /opt/community/backup-db.sh >> /opt/community/logs/backup.log 2>&1
# 备份文件不含密码；凭据由容器内 env 读取，脚本中不出现明文
set -euo pipefail

BACKUP_DIR="/opt/community/backups"
KEEP=7
STAMP="$(date +%Y%m%d_%H%M%S)"
FILE="$BACKUP_DIR/community_$STAMP.sql.gz"

mkdir -p "$BACKUP_DIR"
# 密码从 .env 读取（source 于容器外），仅传给 mysql 客户端进程，不落日志
source /opt/community/.env
docker exec geekstack-mysql mysqldump -ucommunity -p"$MYSQL_PASSWORD" --single-transaction --routines community | gzip > "$FILE"

# 轮转：只保留最近 KEEP 份
ls -1t "$BACKUP_DIR"/community_*.sql.gz | tail -n +$((KEEP + 1)) | xargs -r rm --

echo "[$(date '+%F %T')] backup ok: $FILE ($(du -h "$FILE" | cut -f1))"

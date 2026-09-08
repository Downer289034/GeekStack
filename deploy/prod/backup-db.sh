#!/usr/bin/env bash
# T23 增强版：备份 + 失败告警 + 异地同步（Windows 宿主机 E:\backups\geekstack\）
# 安装 cron：0 3 * * * /opt/community/backup-db.sh >> /opt/community/logs/backup.log 2>&1
set -euo pipefail

BACKUP_DIR="/opt/community/backups"
OFFSITE_DIR="/mnt/e/backups/geekstack"
ALERT_LOG="/opt/community/alerts/alerts.log"
KEEP=7
STAMP="$(date +%Y%m%d_%H%M%S)"
FILE="$BACKUP_DIR/community_$STAMP.sql.gz"

mkdir -p "$BACKUP_DIR" "$OFFSITE_DIR" "$(dirname "$ALERT_LOG")"

# 凭据读取（不写日志）
source /opt/community/.env

# --- 执行备份 ---
DUMP_OK=false
docker exec geekstack-mysql mysqldump \
    -ucommunity -p"$MYSQL_PASSWORD" \
    --single-transaction --routines --no-tablespaces community \
    | gzip > "$FILE" 2>/tmp/backup-stderr.log && DUMP_OK=true

if $DUMP_OK; then
    # heartbeat 记录
    echo "[$(date '+%F %T')] BACKUP_OK file=$FILE size=$(du -h "$FILE" | cut -f1)" >> "$ALERT_LOG"
else
    echo "[$(date '+%F %T')] BACKUP_FAILED stage=mysqldump exit=$? stderr=$(cat /tmp/backup-stderr.log 2>/dev/null | tail -1)" >> "$ALERT_LOG"
fi

# --- 异地同步到 Windows 宿主机（WSL2 /mnt/e） ---
SYNC_OK=false
if [ -d "$OFFSITE_DIR" ]; then
    cp "$FILE" "$OFFSITE_DIR/" && SYNC_OK=true && \
        echo "[$(date '+%F %T')] OFFSITE_SYNC_OK src=$FILE dst=$OFFSITE_DIR/" >> "$ALERT_LOG"
fi

if ! $SYNC_OK; then
    echo "[$(date '+%F %T')] OFFSITE_SYNC_FAILED dst=$OFFSITE_DIR msg=target_not_accessible_or_missing" >> "$ALERT_LOG"
fi

# --- 服务器侧轮转（保留 KEEP 份）---
ls -1t "$BACKUP_DIR"/community_*.sql.gz | tail -n +$((KEEP + 1)) | xargs -r rm -f

# --- 异地轮转（保留 KEEP 份）---
if [ -d "$OFFSITE_DIR" ]; then
    ls -1t "$OFFSITE_DIR"/community_*.sql.gz 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm -f
fi

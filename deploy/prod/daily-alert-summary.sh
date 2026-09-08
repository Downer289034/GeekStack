#!/usr/bin/env bash
# T23 每日告警汇总（cron 每天 08:00 运行：0 8 * * * /opt/community/daily-alert-summary.sh）
# 聚合过去 24h 告警日志为日报文件
set -euo pipefail

ALERT_LOG="/opt/community/alerts/alerts.log"
HEALTH_LOG="/opt/community/alerts/health-check.log"
DAILY_DIR="/opt/community/alerts/daily"
mkdir -p "$DAILY_DIR"

TODAY="$(date +%Y-%m-%d)"
DAILY_FILE="$DAILY_DIR/alert-summary-$TODAY.md"

# 统计过去 24h 的告警
BACKUP_FAIL=$(grep -c "BACKUP_FAILED" "$ALERT_LOG" 2>/dev/null || echo 0)
OFFSITE_FAIL=$(grep -c "OFFSITE_SYNC_FAILED" "$ALERT_LOG" 2>/dev/null || echo 0)
HEALTH_FAIL=$(grep -c "HEALTH_FAIL" "$HEALTH_LOG" 2>/dev/null || echo 0)
BACKUP_OK=$(grep -c "BACKUP_OK" "$ALERT_LOG" 2>/dev/null || echo 0)
HEALTH_OK_COUNT=$(grep -c "HEALTH_OK" "$HEALTH_LOG" 2>/dev/null || echo 0)

TOTAL=$((BACKUP_FAIL + OFFSITE_FAIL + HEALTH_FAIL))

cat > "$DAILY_FILE" <<EOF
# 极客栈 GeekStack 每日健康报告 $TODAY

## 备份
- 成功：$BACKUP_OK 次
- 失败：$BACKUP_FAIL 次
- 异地同步失败：$OFFSITE_FAIL 次

## 健康检查
- 通过：$HEALTH_OK_COUNT 次
- 失败：$HEALTH_FAIL 次

## 告警汇总
- 总告警数：$TOTAL 次
$(if [ "$TOTAL" -gt 0 ]; then
    echo "- ⚠️ 有告警需处理，详见 alerts.log"
else
    echo "- ✅ 24h 内无告警"
fi)

生成时间：$(date '+%F %T')
EOF

echo "[$(date '+%F %T')] DAILY_SUMMARY_GENERATED file=$DAILY_FILE total_alerts=$TOTAL" >> "$ALERT_LOG"

# 轮转：日报告警日志保留 30 天
find "$DAILY_DIR" -name "alert-summary-*.md" -mtime +30 -delete 2>/dev/null || true

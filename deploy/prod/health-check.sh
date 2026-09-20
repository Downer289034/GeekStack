#!/usr/bin/env bash
# T23 健康检查（cron 每 5 分钟：0/5/10/.../55）
# 覆盖："cron 静默失效"（备份新鲜度 >26h 告警）+ 服务健康
set -euo pipefail

ALERT_LOG="/opt/community/alerts/alerts.log"
HEALTH_LOG="/opt/community/alerts/health-check.log"
BACKUP_DIR="/opt/community/backups"
mkdir -p "$(dirname "$ALERT_LOG")"

NOW="$(date '+%F %T')"
STAMP="$(date +%s)"

# --- 1. actuator/health ---
HEALTH_OK=false
if curl -sf http://localhost/actuator/health | grep -q '"status":"UP"'; then
    HEALTH_OK=true
fi

# --- 2. Redis ping ---
REDIS_OK=false
source /opt/community/.env 2>/dev/null || true
if docker exec geekstack-redis redis-cli --no-auth-warning -a "${REDIS_PASSWORD:-}" ping 2>/dev/null | grep -q PONG; then
    REDIS_OK=true
fi

# --- 2b. AI 栈（T-AI-5：可选；未入栈不告警，入栈则必须 UP）---
AI_OK=true
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx geekstack-ai-service; then
    AI_OK=false
    if docker exec geekstack-ai-service python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8090/health', timeout=3)" 2>/dev/null; then
        AI_OK=true
    fi
fi

# --- 3. 备份新鲜度 ---
BACKUP_STALE=true
LATEST_BACKUP=$(ls -1t /opt/community/backups/community_*.sql.gz 2>/dev/null | head -1)
if [ -n "$LATEST_BACKUP" ]; then
    BACKUP_AGE=$((STAMP - $(stat -c %Y "$LATEST_BACKUP")))
    if [ "$BACKUP_AGE" -lt 93600 ]; then
        BACKUP_STALE=false
    fi
fi

# --- 写入 health log（心跳/告警）---
if $HEALTH_OK && $REDIS_OK && $AI_OK && ! $BACKUP_STALE; then
    echo "[$NOW] HEALTH_OK" >> "$HEALTH_LOG"
else
    REASONS=""
    $HEALTH_OK   || REASONS="${REASONS} actuator_down"
    $REDIS_OK    || REASONS="${REASONS} redis_down"
    $AI_OK       || REASONS="${REASONS} ai_service_down"
    ! $BACKUP_STALE || REASONS="${REASONS} backup_stale(>26h)"
    echo "[$NOW] HEALTH_FAIL reasons=$REASONS" >> "$HEALTH_LOG"
    echo "[$NOW] ALERT_HEALTH reasons=$REASONS" >> "$ALERT_LOG"
fi
# --- 4. 日志轮转（health log 保留 7 天）---
find "$(dirname "$HEALTH_LOG")" -name "health-check.log" -mtime +7 -delete 2>/dev/null || true

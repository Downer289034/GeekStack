#!/usr/bin/env bash
# T-AI-5：把仓库 ai-service 同步到开发栈并拉起 qdrant + ai-service（不动 MySQL/Redis 卷）
# 用法：在仓库根执行 bash deploy/dev/sync-ai-stack.sh
set -euo pipefail

SERVER="${AI_STACK_SERVER:-root@172.25.41.137}"
REMOTE_DIR="${AI_STACK_REMOTE:-/opt/community-dev}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "==> 同步 ai-service 源码 → $SERVER:$REMOTE_DIR/ai-service"
ssh "$SERVER" "mkdir -p $REMOTE_DIR/ai-service && find $REMOTE_DIR/ai-service -mindepth 1 -delete"
# Windows OpenSSH 管道 tar 易损坏：落盘再 scp
TAR="$(mktemp /tmp/ai-service-XXXXXX.tar 2>/dev/null || mktemp)"
tar -C "$ROOT/ai-service" \
  --exclude='.venv' --exclude='__pycache__' --exclude='*.pyc' --exclude='.pytest_cache' \
  -cf "$TAR" .
scp "$TAR" "$SERVER:/tmp/ai-service-src.tar"
rm -f "$TAR"
ssh "$SERVER" "tar -C $REMOTE_DIR/ai-service -xf /tmp/ai-service-src.tar && rm -f /tmp/ai-service-src.tar && test -f $REMOTE_DIR/ai-service/Dockerfile"

echo "==> 同步 compose"
scp "$ROOT/deploy/dev/docker-compose.yml" "$SERVER:$REMOTE_DIR/docker-compose.yml"

echo "==> 检查 .env 是否含 AI 键（不打印值）"
ssh "$SERVER" "test -f $REMOTE_DIR/.env && chmod 600 $REMOTE_DIR/.env
  for k in MIMO_API_KEY DASHSCOPE_API_KEY; do
    if grep -q \"^\${k}=\" $REMOTE_DIR/.env 2>/dev/null; then echo \"\$k=present\"; else echo \"\$k=MISSING\"; fi
  done"

echo "==> 仅重建 qdrant + ai-service（--no-deps 不碰 mysql/redis/minio）"
# 早期 spike 的 geekstack-qdrant 占用 6333；停容器但保留卷
ssh "$SERVER" "if docker ps -a --format '{{.Names}}' | grep -qx geekstack-qdrant; then docker stop geekstack-qdrant && docker rm geekstack-qdrant; fi"
ssh "$SERVER" "cd $REMOTE_DIR && docker compose pull qdrant && docker compose build ai-service && docker compose up -d --no-deps qdrant ai-service && docker compose ps qdrant ai-service"

echo "==> 健康检查"
ssh "$SERVER" "curl -sf http://127.0.0.1:8090/health && echo && curl -sf http://127.0.0.1:6333/readyz && echo"

echo "完成。本机 .env 建议：AI_SERVICE_URL=http://172.25.41.137:8090  QDRANT_URL=http://172.25.41.137:6333  QDRANT_COLLECTION=geekstack_community"

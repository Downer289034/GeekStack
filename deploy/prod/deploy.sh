#!/usr/bin/env bash
# 一键部署脚本：本机构建 jar + 前端 dist → 传服务器 → compose 拉起
# 用法：在仓库根目录执行 bash deploy/prod/deploy.sh
set -euo pipefail

SERVER="root@172.25.41.137"
REMOTE_DIR="/opt/community"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "==> 1/5 构建后端 jar"
( cd "$ROOT/backend" && mvn -B -q clean package -DskipTests )

echo "==> 2/5 构建前端 dist"
( cd "$ROOT/frontend" && npm run build )

echo "==> 3/5 传输产物到服务器"
ssh "$SERVER" "mkdir -p $REMOTE_DIR/backend-build $REMOTE_DIR/frontend-dist $REMOTE_DIR/nginx/conf.d $REMOTE_DIR/logs/backend $REMOTE_DIR/backups $REMOTE_DIR/certbot/conf $REMOTE_DIR/certbot/www"
scp "$ROOT"/backend/target/community-backend-*.jar "$SERVER:$REMOTE_DIR/backend-build/"
scp "$ROOT"/deploy/prod/Dockerfile "$SERVER:$REMOTE_DIR/"
scp -r "$ROOT"/frontend/dist/* "$SERVER:$REMOTE_DIR/frontend-dist/"
scp "$ROOT"/deploy/prod/docker-compose.yml "$ROOT"/deploy/prod/nginx/conf.d/geekstack.conf "$ROOT"/deploy/prod/backup-db.sh "$SERVER:$REMOTE_DIR/" 2>/dev/null || true
scp "$ROOT"/deploy/prod/nginx/conf.d/geekstack.conf "$SERVER:$REMOTE_DIR/nginx/conf.d/"

echo "==> 4/5 服务器生成 .env（已存在则保留，不覆盖）"
ssh "$SERVER" "cd $REMOTE_DIR && if [ ! -f .env ]; then echo 'JWT_SECRET='\"$(openssl rand -hex 32)\" > .env && grep -E 'MYSQL_PASSWORD|REDIS_PASSWORD' /dev/null >> .env; fi"
# 注意：开发版 .env 已有 MYSQL/REDIS 密码，直接复用；JWT_SECRET 若缺失则补
ssh "$SERVER" "cd $REMOTE_DIR && grep -q '^JWT_SECRET=' .env || echo \"JWT_SECRET=\$(openssl rand -hex 32)\" >> .env; chmod 600 .env"

echo "==> 5/5 compose 拉起（若开发版容器在跑，先停以释放 3306/6379）"
ssh "$SERVER" "cd $REMOTE_DIR && (docker compose down || true) && docker compose -f docker-compose.yml up -d --build && docker compose ps"

echo "部署完成。验证：curl http://<服务器>/actuator/health"

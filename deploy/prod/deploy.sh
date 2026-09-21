#!/usr/bin/env bash
# 同版本部署：同一 commit 构建 jar + 前端 dist → 只重建 backend/nginx，不动 MySQL/Redis 卷
# 用法：在仓库根目录执行 bash deploy/prod/deploy.sh
# 禁止：docker compose down / down -v（会扩大误伤面）
# 禁止：docker cp 热更 /app/app.jar（含 T-M4-1 AdminController 等变更须本脚本重建 geekstack/backend 镜像）
set -euo pipefail

SERVER="root@172.25.41.137"
REMOTE_DIR="/opt/community"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMMIT="$(git -C "$ROOT" rev-parse --short HEAD)"

echo "==> 0/6 部署纪律：前后端同 commit=$COMMIT（禁止只更一侧）"

echo "==> 1/6 构建后端 jar"
( cd "$ROOT/backend" && mvn -B -q clean package -DskipTests )

echo "==> 2/6 构建前端 dist（与 jar 同一 commit）"
( cd "$ROOT/frontend" && npm run build )

echo "==> 3/6 传输产物到服务器"
ssh "$SERVER" "mkdir -p $REMOTE_DIR/backend-build $REMOTE_DIR/frontend-dist $REMOTE_DIR/nginx/conf.d $REMOTE_DIR/logs/backend $REMOTE_DIR/backups $REMOTE_DIR/certbot/conf $REMOTE_DIR/certbot/www $REMOTE_DIR/ai-service"
scp "$ROOT"/backend/target/community-backend-*.jar "$SERVER:$REMOTE_DIR/backend-build/"
scp "$ROOT"/deploy/prod/Dockerfile "$SERVER:$REMOTE_DIR/"
# 清空旧 dist 再覆盖，避免残留旧 hash 资源
ssh "$SERVER" "rm -rf $REMOTE_DIR/frontend-dist/*"
scp -r "$ROOT"/frontend/dist/* "$SERVER:$REMOTE_DIR/frontend-dist/"
scp "$ROOT"/deploy/prod/nginx/conf.d/geekstack.conf "$SERVER:$REMOTE_DIR/nginx/conf.d/"
# T-AI-5：同步 ai-service 构建上下文（不含 .venv）
rsync -az --delete \
  --exclude '.venv' --exclude '__pycache__' --exclude '*.pyc' --exclude '.pytest_cache' \
  "$ROOT/ai-service/" "$SERVER:$REMOTE_DIR/ai-service/"
# compose 仅在显式需要时同步；默认不覆盖，避免未就绪服务拖垮现网
# scp "$ROOT"/deploy/prod/docker-compose.yml "$SERVER:$REMOTE_DIR/docker-compose.yml"
ssh "$SERVER" "echo $COMMIT > $REMOTE_DIR/DEPLOY_COMMIT; chmod 600 $REMOTE_DIR/.env 2>/dev/null || true"

echo "==> 4/6 部署前确认 MySQL 卷（必须仍是 community_mysql-data）"
ssh "$SERVER" "VOL=\$(docker inspect geekstack-mysql --format '{{range .Mounts}}{{.Name}}{{end}}'); echo MYSQL_VOL=\$VOL; test \"\$VOL\" = 'community_mysql-data'"

echo "==> 5/6 只重建 backend +（若已入栈）ai-service + 刷新 nginx（--no-deps，不动 mysql/redis）"
ssh "$SERVER" "cd $REMOTE_DIR && docker compose build backend && docker compose up -d --no-deps backend && (docker compose config --services | grep -qx ai-service && docker compose build ai-service && docker compose up -d --no-deps qdrant ai-service || true) && docker compose up -d --no-deps nginx && docker compose ps"

echo "==> 6/6 冒烟"
ssh "$SERVER" "curl -sf http://127.0.0.1/actuator/health && echo && curl -sI http://127.0.0.1/api/v1/oauth/login/GITEE | head -n 5 && (docker exec geekstack-ai-service python -c \"import urllib.request; print(urllib.request.urlopen('http://127.0.0.1:8090/health').read().decode())\" 2>/dev/null || echo 'ai-service: not-in-stack') && echo DEPLOY_COMMIT=\$(cat $REMOTE_DIR/DEPLOY_COMMIT)"

echo "部署完成（commit=$COMMIT）。若 OAuth Location 的 client_id 为空，检查服务器 .env 是否有非空 GITEE_CLIENT_ID（注意本地 .env 勿留空重复键）。"

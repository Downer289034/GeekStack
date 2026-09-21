#!/usr/bin/env bash
# T-SEED-1：向 community-dev MySQL 灌入真实热门种子（幂等，仅 real_*）
# 用法（仓库根或 deploy/dev）：
#   bash deploy/dev/seed-real-hot-data.sh
# 依赖：同目录 .env（MYSQL_PASSWORD）或 /opt/community-dev/.env；docker 容器 community-mysql 运行中
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="${DIR}/seed-real-hot-data.sql"

if [[ -f "${DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${DIR}/.env"
  set +a
elif [[ -f /opt/community-dev/.env ]]; then
  set -a
  # shellcheck disable=SC1091
  source /opt/community-dev/.env
  set +a
fi

: "${MYSQL_PASSWORD:?MYSQL_PASSWORD 未设置（请在 deploy/dev/.env 或环境中提供）}"

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "缺少 ${SQL_FILE}" >&2
  exit 1
fi

echo "[seed-real] 执行 ${SQL_FILE} → community-mysql/community"
docker exec -i -e MYSQL_PWD="${MYSQL_PASSWORD}" community-mysql \
  mysql -ucommunity --default-character-set=utf8mb4 community < "${SQL_FILE}"
echo "[seed-real] 完成（账号 real_alice / real_bob / real_carol；口令见 docs/assets/seed-catalog/README.md）"

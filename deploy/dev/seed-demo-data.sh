#!/usr/bin/env bash
# T-BE-3：向 community-dev MySQL 灌入演示数据（幂等）
# 用法（在服务器 /opt/community-dev 或本机仓库 deploy/dev）：
#   bash seed-demo-data.sh
# 依赖：同目录 .env（MYSQL_PASSWORD）或已 export；docker 容器 community-mysql 运行中
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="${DIR}/seed-demo-data.sql"

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

echo "[seed] 执行 ${SQL_FILE} → community-mysql/community"
docker exec -i -e MYSQL_PWD="${MYSQL_PASSWORD}" community-mysql \
  mysql -ucommunity --default-character-set=utf8mb4 community < "${SQL_FILE}"
echo "[seed] 完成"

#!/usr/bin/env bash
# 一次性：把本机根 .env 的 AI 相关键合并进服务器 /opt/community-dev/.env（不打印密钥）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ENVF="$ROOT/.env"
test -f "$ENVF"
SNIPPET="$(mktemp)"
trap 'rm -f "$SNIPPET"' EXIT

grep -E '^(MIMO_|DASHSCOPE_|EMBEDDING_|QDRANT_|AI_SERVICE_URL|AI_RATE_|RAG_|RETRIEVE_)' "$ENVF" > "$SNIPPET" || true
grep -q '^QDRANT_COLLECTION=' "$SNIPPET" || echo 'QDRANT_COLLECTION=geekstack_community' >> "$SNIPPET"
grep -q '^EMBEDDING_MODEL=' "$SNIPPET" || echo 'EMBEDDING_MODEL=text-embedding-v3' >> "$SNIPPET"
grep -q '^EMBEDDING_DIM=' "$SNIPPET" || echo 'EMBEDDING_DIM=1024' >> "$SNIPPET"

scp "$SNIPPET" root@172.25.41.137:/tmp/ai-env-snippet.env
ssh root@172.25.41.137 'set -e
  ENV=/opt/community-dev/.env
  cp "$ENV" "$ENV.bak.$(date +%s)"
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    key=${line%%=*}
    if grep -q "^${key}=" "$ENV"; then
      sed -i "/^${key}=/d" "$ENV"
    fi
    printf "%s\n" "$line" >> "$ENV"
  done < /tmp/ai-env-snippet.env
  chmod 600 "$ENV"
  rm -f /tmp/ai-env-snippet.env
  for k in MIMO_API_KEY DASHSCOPE_API_KEY QDRANT_COLLECTION EMBEDDING_MODEL; do
    if grep -q "^${k}=" "$ENV"; then echo "$k=present"; else echo "$k=MISSING"; fi
  done
'
echo merge-done

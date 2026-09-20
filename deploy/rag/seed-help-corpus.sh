#!/usr/bin/env bash
# 一次性灌入平台帮助语料到 RAG 索引（T-AI-1）。
# 用法：先启动 backend，并设置 community.rag.seed-help-on-startup=true 重启一次；
# 或在已运行实例上依赖 classpath 资源（同内容见 backend/src/main/resources/rag/help-corpus.json）。
# 本脚本仅复制/校验 JSON，实际灌库由 HelpCorpusSeedRunner 完成。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC="$ROOT/deploy/rag/help-corpus.json"
test -f "$SRC"
python -c "import json; json.load(open(r'$SRC', encoding='utf-8')); print('help-corpus.json OK')"
echo "将 community.rag.seed-help-on-startup=true 后重启 backend 即可灌库。"

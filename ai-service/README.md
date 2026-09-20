# GeekStack AI Service（T-AI-0 spike → 演进 T-AI-2）

独立 Python 服务：FastAPI + LangGraph RAG + Qdrant + DashScope `text-embedding-v3` + 小米 MiMo（OpenAI 兼容）。

## 快速开始（spike）

```bash
# 1. 根目录 .env 已含 MIMO_API_KEY + DASHSCOPE_API_KEY（勿提交）
# 2. Qdrant：默认 :memory: 无需 Docker；联调可 docker compose up -d 并设 QDRANT_URL=http://127.0.0.1:6333
cd ai-service

# 3. 虚拟环境
py -3.12 -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt

# 4. 端到端：分块→入库→问答
python -m scripts.run_spike

# 5. 四问实测（thinking / 分块 / DashScope / 延迟 P95 / 成本）
python -m scripts.bench_report

# 6. HTTP 骨架（可演进 T-AI-2 SSE）
uvicorn app.main:app --reload --port 8090
```

## 目录

| 路径 | 职责 |
|------|------|
| `app/config.py` | 环境配置 |
| `app/chunking.py` | Markdown 分块（token 窗口） |
| `app/embedding.py` | DashScope text-embedding-v3 |
| `app/vectorstore.py` | Qdrant 读写 + 统一 metadata schema |
| `app/llm.py` | MiMo Chat Completions + thinking 开关 |
| `app/graph/rag_graph.py` | LangGraph：retrieve → generate |
| `app/main.py` | FastAPI `/health` `/v1/ask` `/v1/index` |
| `data/sample_corpus.json` | 10 条模拟社区语料（`jkz_` 前缀） |
| `scripts/` | spike / 基准脚本 |
| `reports/raw/` | 实测 JSON（本地生成，可入 docs 摘要） |

## 硬约束

- Key 只读环境变量 / 仓库根 `.env`
- 回答必须带来源链接（不足则如实说明）
- 只索引公开内容（spike 语料已是公开模拟）
- embedding 双方同源：`text-embedding-v3`（1024 维），型号可配置

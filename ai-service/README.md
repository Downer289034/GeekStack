# GeekStack AI Service（T-AI-0 spike → T-AI-2 问答）

独立 Python 服务：FastAPI + LangGraph RAG + Qdrant + DashScope `text-embedding-v3` + 小米 MiMo（OpenAI 兼容）。

## 快速开始

```bash
# 1. 根目录 .env：MIMO_API_KEY + DASHSCOPE_API_KEY
cd ai-service
.\.venv\Scripts\activate   # 或新建 venv 后 pip install -r requirements.txt

# 2. 三分支单测（无 Key）
python -m unittest tests.test_retrieve -v

# 3. 端到端 spike / 10 问评测（需 Key）
python -m scripts.run_spike
python -m scripts.eval_ask

# 4. HTTP：同步 + SSE
uvicorn app.main:app --reload --port 8090
# POST /v1/ask
# POST /v1/ask/stream  → event: token | sources | done
```

## 目录

| 路径 | 职责 |
|------|------|
| `app/retrieve.py` | 空结果 / 正常 / 超阈值三分支 |
| `app/graph/rag_graph.py` | rewrite → retrieve → generate；SSE 事件迭代 |
| `app/llm.py` | MiMo 同步 + 流式 + 改写 |
| `app/main.py` | `/v1/ask`、`/v1/ask/stream` |
| `tests/test_retrieve.py` | 三分支单测 |
| `scripts/eval_ask.py` | 10 问评测写 `docs/assets/t-ai-2/eval.json` |

## 硬约束

- Key 只读环境变量；回答必带来源或明确「无法确定」
- thinking 默认 disabled（T-AI-0 拍板）
- 检索阈值默认 0.35；payload 对齐 schema v1.1

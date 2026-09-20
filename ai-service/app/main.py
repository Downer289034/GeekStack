"""FastAPI 入口：spike 可用；T-AI-2 在 /v1/ask 上扩展 SSE。"""

from __future__ import annotations

import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

from app.chunking import chunk_document
from app.config import get_settings
from app.embedding import get_embedder
from app.graph.rag_graph import build_rag_graph
from app.models import AskRequest, AskResponse, CorpusDoc
from app.vectorstore import VectorStore

_graph = None
_store: VectorStore | None = None


@asynccontextmanager
async def lifespan(_: FastAPI):
    global _graph, _store
    settings = get_settings()
    _store = VectorStore(settings)
    _store.ensure_collection(recreate=False)
    _graph = build_rag_graph(store=_store)
    yield


app = FastAPI(title="GeekStack AI Service", version="0.1.0-spike", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "UP", "service": "ai-service", "phase": "T-AI-0"}


@app.post("/v1/index")
def index_docs(docs: list[CorpusDoc], recreate: bool = False):
    """批量索引（spike / 本地灌库）。T-AI-1 将由 Java 事件管道调用同类逻辑。"""
    settings = get_settings()
    store = _store or VectorStore(settings)
    store.ensure_collection(recreate=recreate)
    embedder = get_embedder()
    all_chunks = []
    for doc in docs:
        all_chunks.extend(
            chunk_document(
                doc,
                size_tokens=settings.chunk_size_tokens,
                overlap_tokens=settings.chunk_overlap_tokens,
            )
        )
    if not all_chunks:
        return {"indexed_chunks": 0}
    vectors = embedder.embed_documents([c.text for c in all_chunks])
    n = store.upsert_chunks(all_chunks, vectors)
    return {"indexed_chunks": n, "docs": len(docs)}


@app.post("/v1/ask", response_model=AskResponse)
def ask(req: AskRequest):
    """同步问答（spike）。T-AI-2 增加 SSE 流式。"""
    if _graph is None:
        raise HTTPException(503, "graph not ready")
    settings = get_settings()
    t0 = time.perf_counter()
    out = _graph.invoke(
        {
            "question": req.question,
            "thinking": req.thinking or settings.mimo_thinking,
            "top_k": req.top_k or settings.retrieve_top_k,
        }
    )
    total_ms = (time.perf_counter() - t0) * 1000
    return AskResponse(
        answer=out.get("answer") or "",
        sources=out.get("sources") or [],
        latency_ms=out.get("latency_ms") or total_ms,
        usage=out.get("usage") or {},
        thinking=out.get("thinking") or settings.mimo_thinking,
    )


@app.get("/")
def root():
    return JSONResponse(
        {
            "service": "geekstack-ai-service",
            "docs": "/docs",
            "health": "/health",
        }
    )

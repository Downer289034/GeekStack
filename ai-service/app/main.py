"""FastAPI 入口：同步问答 + SSE 流式（T-AI-2）。"""

from __future__ import annotations

import json
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from sse_starlette.sse import EventSourceResponse

from app.chunking import chunk_document
from app.config import get_settings
from app.embedding import get_embedder
from app.graph.rag_graph import build_rag_graph, iter_answer_events, prepare_retrieval
from app.llm import MiMoClient
from app.models import AskRequest, AskResponse, CorpusDoc
from app.vectorstore import VectorStore

_graph = None
_store: VectorStore | None = None
_llm: MiMoClient | None = None


@asynccontextmanager
async def lifespan(_: FastAPI):
    global _graph, _store, _llm
    settings = get_settings()
    _store = VectorStore(settings)
    _store.ensure_collection(recreate=False)
    _llm = MiMoClient(settings) if settings.mimo_api_key else None
    if _llm is not None:
        _graph = build_rag_graph(store=_store, llm=_llm, settings=settings)
    yield


app = FastAPI(title="GeekStack AI Service", version="0.2.0", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "UP", "service": "ai-service", "phase": "T-AI-5"}


@app.post("/v1/index")
def index_docs(docs: list[CorpusDoc], recreate: bool = False):
    """批量索引（本地灌库）。生产索引由 Java T-AI-1 写入同一 schema。"""
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
    """同步问答：rewrite → retrieve → generate。"""
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
        retrieval=out.get("branch") or "ok",
    )


@app.post("/v1/ask/stream")
def ask_stream(req: AskRequest):
    """SSE：token / sources / done。空检索与超阈值直接兜底，不调用生成模型。"""
    if _store is None:
        raise HTTPException(503, "store not ready")
    settings = get_settings()
    state = prepare_retrieval(
        question=req.question,
        store=_store,
        embedder=get_embedder(),
        llm=_llm,
        settings=settings,
        thinking=req.thinking or settings.mimo_thinking,
        top_k=req.top_k or settings.retrieve_top_k,
    )

    def gen():
        for event in iter_answer_events(state, _llm):
            yield {
                "event": event["event"],
                "data": json.dumps(event["data"], ensure_ascii=False),
            }

    return EventSourceResponse(gen())


@app.get("/")
def root():
    return JSONResponse(
        {
            "service": "geekstack-ai-service",
            "docs": "/docs",
            "health": "/health",
        }
    )

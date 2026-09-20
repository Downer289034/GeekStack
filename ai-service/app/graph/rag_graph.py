"""LangGraph RAG：rewrite → retrieve（阈值过滤）→ generate。"""

from __future__ import annotations

from typing import Any, Iterator, TypedDict

from langgraph.graph import END, StateGraph

from app.config import Settings, get_settings
from app.embedding import Embedder, get_embedder
from app.llm import MiMoClient
from app.models import SourceRef
from app.retrieve import (
    BRANCH_OK,
    FALLBACK_ANSWER,
    classify_retrieval,
)
from app.vectorstore import VectorStore


class RagState(TypedDict, total=False):
    question: str
    rewritten_question: str
    thinking: str
    top_k: int
    score_threshold: float
    branch: str
    contexts: list[str]
    sources: list[SourceRef]
    answer: str
    usage: dict[str, Any]
    latency_ms: float


def _context_block(ref: SourceRef, text: str) -> str:
    return f"[{ref.title}]({ref.url})\n相似度={ref.score:.3f}\n{text}"


def build_nodes(
    *,
    store: VectorStore,
    embedder: Embedder,
    llm: MiMoClient | None,
    settings: Settings,
):
    def rewrite(state: RagState) -> RagState:
        question = state["question"]
        rewritten = question
        if llm is not None:
            rewritten = llm.rewrite(question) or question
        return {"rewritten_question": rewritten}

    def retrieve(state: RagState) -> RagState:
        query = state.get("rewritten_question") or state["question"]
        top_k = int(state.get("top_k") or settings.retrieve_top_k)
        threshold = float(
            state.get("score_threshold")
            if state.get("score_threshold") is not None
            else settings.retrieve_score_threshold
        )
        vec = embedder.embed_query(query)
        hits = store.search(vec, top_k=top_k)
        branch, kept = classify_retrieval(hits, threshold)
        contexts = [_context_block(ref, text) for ref, text in kept]
        sources = [ref for ref, _ in kept]
        return {"branch": branch, "contexts": contexts, "sources": sources}

    def generate(state: RagState) -> RagState:
        if state.get("branch") != BRANCH_OK or not state.get("contexts"):
            return {
                "answer": FALLBACK_ANSWER,
                "usage": {},
                "latency_ms": 0.0,
                "thinking": state.get("thinking") or settings.mimo_thinking,
            }
        if llm is None:
            raise RuntimeError("LLM 未配置")
        result = llm.chat(
            user_message=state["question"],
            context_blocks=state.get("contexts") or [],
            thinking=state.get("thinking"),
        )
        answer = result["content"]
        sources = state.get("sources") or []
        if sources and "http" not in answer:
            lines = ["", "### 来源"]
            for s in sources:
                lines.append(f"- [{s.title}]({s.url})")
            answer = answer.rstrip() + "\n" + "\n".join(lines)
        return {
            "answer": answer,
            "usage": result["usage"],
            "latency_ms": result["latency_ms"],
            "thinking": result["thinking"],
        }

    return rewrite, retrieve, generate


def build_rag_graph(
    *,
    store: VectorStore | None = None,
    embedder: Embedder | None = None,
    llm: MiMoClient | None = None,
    settings: Settings | None = None,
):
    settings = settings or get_settings()
    store = store or VectorStore(settings)
    embedder = embedder or get_embedder()
    if llm is None:
        llm = MiMoClient(settings)
    rewrite, retrieve, generate = build_nodes(
        store=store, embedder=embedder, llm=llm, settings=settings
    )
    graph = StateGraph(RagState)
    graph.add_node("rewrite", rewrite)
    graph.add_node("retrieve", retrieve)
    graph.add_node("generate", generate)
    graph.set_entry_point("rewrite")
    graph.add_edge("rewrite", "retrieve")
    graph.add_edge("retrieve", "generate")
    graph.add_edge("generate", END)
    return graph.compile()


def prepare_retrieval(
    *,
    question: str,
    store: VectorStore,
    embedder: Embedder,
    llm: MiMoClient | None,
    settings: Settings | None = None,
    thinking: str | None = None,
    top_k: int | None = None,
) -> RagState:
    """rewrite → retrieve 子图，供 SSE 在 generate 前分流。"""
    settings = settings or get_settings()
    rewrite, retrieve, _ = build_nodes(
        store=store, embedder=embedder, llm=llm, settings=settings
    )
    graph = StateGraph(RagState)
    graph.add_node("rewrite", rewrite)
    graph.add_node("retrieve", retrieve)
    graph.set_entry_point("rewrite")
    graph.add_edge("rewrite", "retrieve")
    graph.add_edge("retrieve", END)
    compiled = graph.compile()
    return compiled.invoke(
        {
            "question": question,
            "thinking": thinking or settings.mimo_thinking,
            "top_k": top_k or settings.retrieve_top_k,
        }
    )


def iter_answer_events(state: RagState, llm: MiMoClient | None) -> Iterator[dict[str, Any]]:
    """SSE 事件：token / sources / done。空检索与超阈值不调用 LLM。"""
    sources = [
        {"content_id": s.content_id, "title": s.title, "url": s.url, "score": s.score}
        for s in (state.get("sources") or [])
    ]
    if state.get("branch") != BRANCH_OK or not state.get("contexts"):
        yield {"event": "token", "data": {"delta": FALLBACK_ANSWER}}
        yield {"event": "sources", "data": {"sources": []}}
        yield {
            "event": "done",
            "data": {
                "branch": state.get("branch") or "empty",
                "latency_ms": 0.0,
                "usage": {},
            },
        }
        return
    if llm is None:
        raise RuntimeError("LLM 未配置")
    usage: dict[str, Any] = {}
    latency = 0.0
    thinking = state.get("thinking") or "disabled"
    for piece in llm.stream_chat(
        user_message=state["question"],
        context_blocks=state.get("contexts") or [],
        thinking=thinking,
    ):
        if piece.get("type") == "delta":
            yield {"event": "token", "data": {"delta": piece.get("text") or ""}}
        elif piece.get("type") == "done":
            usage = piece.get("usage") or {}
            latency = float(piece.get("latency_ms") or 0.0)
            thinking = piece.get("thinking") or thinking
    yield {"event": "sources", "data": {"sources": sources}}
    yield {
        "event": "done",
        "data": {"branch": BRANCH_OK, "latency_ms": latency, "usage": usage, "thinking": thinking},
    }

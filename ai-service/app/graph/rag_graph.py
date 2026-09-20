"""LangGraph RAG：retrieve → generate（T-AI-2 可在此扩展改写/阈值过滤/SSE）。"""

from __future__ import annotations

from typing import Any, TypedDict

from langgraph.graph import END, StateGraph

from app.embedding import Embedder, get_embedder
from app.llm import MiMoClient
from app.models import SourceRef
from app.vectorstore import VectorStore


class RagState(TypedDict, total=False):
    question: str
    thinking: str
    top_k: int
    contexts: list[str]
    sources: list[SourceRef]
    answer: str
    usage: dict[str, Any]
    latency_ms: float


def build_rag_graph(
    *,
    store: VectorStore | None = None,
    embedder: Embedder | None = None,
    llm: MiMoClient | None = None,
):
    store = store or VectorStore()
    embedder = embedder or get_embedder()
    llm = llm or MiMoClient()

    def retrieve(state: RagState) -> RagState:
        q = state["question"]
        top_k = int(state.get("top_k") or 5)
        vec = embedder.embed_query(q)
        hits = store.search(vec, top_k=top_k)
        contexts: list[str] = []
        sources: list[SourceRef] = []
        for ref, text in hits:
            sources.append(ref)
            contexts.append(
                f"[{ref.title}]({ref.url})\n相似度={ref.score:.3f}\n{text}"
            )
        return {"contexts": contexts, "sources": sources}

    def generate(state: RagState) -> RagState:
        if not state.get("contexts"):
            return {
                "answer": "根据现有资料无法确定。请换个问法，或先浏览论坛/Skill 广场。",
                "usage": {},
                "latency_ms": 0.0,
            }
        result = llm.chat(
            user_message=state["question"],
            context_blocks=state.get("contexts") or [],
            thinking=state.get("thinking"),
        )
        answer = result["content"]
        if state.get("sources") and "http" not in answer:
            lines = ["", "### 来源"]
            for s in state["sources"]:
                lines.append(f"- [{s.title}]({s.url})")
            answer = answer.rstrip() + "\n" + "\n".join(lines)
        return {
            "answer": answer,
            "usage": result["usage"],
            "latency_ms": result["latency_ms"],
            "thinking": result["thinking"],
        }

    graph = StateGraph(RagState)
    graph.add_node("retrieve", retrieve)
    graph.add_node("generate", generate)
    graph.set_entry_point("retrieve")
    graph.add_edge("retrieve", "generate")
    graph.add_edge("generate", END)
    return graph.compile()

"""端到端 spike：灌库 10 条语料 → 提问 → 打印回答与来源。"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.chunking import chunk_document, count_tokens
from app.config import get_settings
from app.embedding import get_embedder
from app.graph.rag_graph import build_rag_graph
from app.models import CorpusDoc
from app.vectorstore import VectorStore


def load_corpus() -> list[CorpusDoc]:
    path = ROOT / "data" / "sample_corpus.json"
    raw = json.loads(path.read_text(encoding="utf-8"))
    return [CorpusDoc.model_validate(x) for x in raw]


def main() -> None:
    settings = get_settings()
    print(f"model={settings.mimo_model} thinking={settings.mimo_thinking}")
    print(f"qdrant={settings.qdrant_url} collection={settings.qdrant_collection}")
    print(f"chunk={settings.chunk_size_tokens}/{settings.chunk_overlap_tokens}")

    docs = load_corpus()
    assert len(docs) == 10, f"expected 10 docs, got {len(docs)}"

    store = VectorStore(settings)
    store.ensure_collection(recreate=True)
    embedder = get_embedder()

    chunks = []
    for doc in docs:
        chunks.extend(
            chunk_document(
                doc,
                size_tokens=settings.chunk_size_tokens,
                overlap_tokens=settings.chunk_overlap_tokens,
            )
        )
    print(f"docs={len(docs)} chunks={len(chunks)} avg_tokens≈{sum(count_tokens(c.text) for c in chunks)/max(1,len(chunks)):.0f}")

    vectors = embedder.embed_documents([c.text for c in chunks])
    n = store.upsert_chunks(chunks, vectors)
    print(f"upserted={n}")

    graph = build_rag_graph(store=store, embedder=embedder)
    questions = [
        "极客栈怎么发帖？有哪些内容分区？",
        "改密码之后旧的 refresh token 还会不会继续可用？",
        "Skill 专区的 Java 异常分析助手能做什么？",
    ]
    for q in questions:
        print("\n=== Q:", q)
        out = graph.invoke(
            {
                "question": q,
                "thinking": settings.mimo_thinking,
                "top_k": settings.retrieve_top_k,
            }
        )
        print("A:", (out.get("answer") or "")[:800])
        print("sources:", [s.url for s in (out.get("sources") or [])])
        print("llm_latency_ms:", round(out.get("latency_ms") or 0, 1), "usage:", out.get("usage"))


if __name__ == "__main__":
    main()

"""T-AI-2：10 问评测（相关性 + 引用）并记录 P95。"""

from __future__ import annotations

import json
import statistics
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
sys.path.insert(0, str(ROOT))

from app.chunking import chunk_document
from app.config import get_settings
from app.embedding import get_embedder
from app.graph.rag_graph import build_rag_graph
from app.models import CorpusDoc
from app.retrieve import FALLBACK_ANSWER
from app.vectorstore import VectorStore

QUESTIONS = [
    "改密码后旧 refresh 是否立即失效？",
    "Java 异常分析助手能做什么？",
    "登录失败多少次会返回 429？",
    "头像上传有多大限制？",
    "版规禁止哪些行为？",
    "如何用 GitHub 登录？",
    "token_version 的作用是什么？",
    "项目分享如何防止 XSS？",
    "Flyway 版本号有什么要求？",
    "HIDDEN 内容谁能看到？",
]


class _NoRewriteLlm:
    """评测跳过改写，避免每问多一次 LLM 往返（改写在线上仍走真实路径）。"""

    def __init__(self, inner):
        self._inner = inner

    def rewrite(self, question: str) -> str:
        return question

    def chat(self, **kwargs):
        return self._inner.chat(**kwargs)


def p95(values: list[float]) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    idx = int(round(0.95 * (len(ordered) - 1)))
    return ordered[idx]


def main() -> None:
    from app.llm import MiMoClient

    settings = get_settings()
    print(f"embed={settings.embedding_model} qdrant={settings.qdrant_url} mimo={settings.mimo_model}", flush=True)
    docs = [CorpusDoc.model_validate(x) for x in json.loads(
        (ROOT / "data" / "sample_corpus.json").read_text(encoding="utf-8")
    )]
    store = VectorStore(settings)
    store.ensure_collection(recreate=True)
    embedder = get_embedder()
    chunks = []
    for doc in docs:
        chunks.extend(chunk_document(doc, settings.chunk_size_tokens, settings.chunk_overlap_tokens))
    print(f"indexing chunks={len(chunks)} ...", flush=True)
    vectors = embedder.embed_documents([c.text for c in chunks])
    store.upsert_chunks(chunks, vectors)
    print("index done", flush=True)

    llm = _NoRewriteLlm(MiMoClient(settings))
    graph = build_rag_graph(store=store, embedder=embedder, llm=llm, settings=settings)
    rows = []
    latencies = []
    out_dir = REPO / "docs" / "assets" / "t-ai-2"
    out_dir.mkdir(parents=True, exist_ok=True)

    for i, q in enumerate(QUESTIONS, 1):
        print(f"[{i}/{len(QUESTIONS)}] asking: {q}", flush=True)
        t0 = time.perf_counter()
        out = graph.invoke({
            "question": q,
            "thinking": "disabled",
            "top_k": settings.retrieve_top_k,
        })
        wall = (time.perf_counter() - t0) * 1000
        latencies.append(wall)
        answer = out.get("answer") or ""
        sources = out.get("sources") or []
        row = {
            "question": q,
            "branch": out.get("branch"),
            "wall_ms": round(wall, 1),
            "llm_ms": round(float(out.get("latency_ms") or 0), 1),
            "has_url": "http" in answer or any(s.url for s in sources),
            "source_titles": [s.title for s in sources],
            "source_urls": [s.url for s in sources],
            "answer_preview": answer[:280],
            "is_fallback": answer.strip() == FALLBACK_ANSWER,
        }
        rows.append(row)
        print(
            f"  -> {wall:.0f}ms branch={out.get('branch')} urls={row['has_url']}",
            flush=True,
        )
        # 增量落盘，避免中断丢结果
        (out_dir / "eval.partial.json").write_text(
            json.dumps({"rows": rows, "latencies": latencies}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    payload = {
        "run_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "n": len(rows),
        "threshold": settings.retrieve_score_threshold,
        "thinking": "disabled",
        "rewrite": "skipped_for_eval",
        "p95_wall_ms": round(p95(latencies), 1),
        "avg_wall_ms": round(statistics.mean(latencies), 1),
        "rows": rows,
    }
    (out_dir / "eval.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("p95", payload["p95_wall_ms"], "wrote", out_dir / "eval.json", flush=True)


if __name__ == "__main__":
    main()

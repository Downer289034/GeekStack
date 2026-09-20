"""T-AI-0 四问实测：分块策略、thinking 对比、DashScope embedding、成本、P95 延迟。"""

from __future__ import annotations

import json
import statistics
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.chunking import chunk_document, count_tokens
from app.config import get_settings
from app.embedding import Embedder, get_embedder
from app.llm import MiMoClient
from app.models import CorpusDoc
from app.vectorstore import VectorStore

REPORT_DIR = ROOT / "reports"
RAW_DIR = REPORT_DIR / "raw"

SCHEMA_REQUIRED = [
    "content_id",
    "source_system",
    "content_type",
    "text_url",
    "author_id",
    "created_at",
    "visibility",
    "embedding_model",
    "text",
]


def load_corpus() -> list[CorpusDoc]:
    raw = json.loads((ROOT / "data" / "sample_corpus.json").read_text(encoding="utf-8"))
    return [CorpusDoc.model_validate(x) for x in raw]


def p95(values: list[float]) -> float:
    if not values:
        return 0.0
    if len(values) == 1:
        return values[0]
    ordered = sorted(values)
    idx = int(round(0.95 * (len(ordered) - 1)))
    return ordered[idx]


def bench_chunking(docs: list[CorpusDoc]) -> dict:
    configs = [
        (256, 32),
        (512, 50),
        (1024, 100),
    ]
    rows = []
    for size, overlap in configs:
        chunks = []
        for d in docs:
            chunks.extend(chunk_document(d, size_tokens=size, overlap_tokens=overlap))
        lengths = [count_tokens(c.text) for c in chunks]
        rows.append(
            {
                "size": size,
                "overlap": overlap,
                "chunk_count": len(chunks),
                "avg_tokens": round(sum(lengths) / max(1, len(lengths)), 1),
                "max_tokens": max(lengths) if lengths else 0,
                "note": "推荐基线" if size == 512 else "",
            }
        )
    return {"strategies": rows, "recommendation": "512/50：块数与上下文长度折中，适合 top-5 RAG"}


def bench_embedding(docs: list[CorpusDoc]) -> dict:
    settings = get_settings()
    embedder = Embedder(settings)  # 独立实例，便于统计
    chunks = []
    for d in docs:
        chunks.extend(chunk_document(d, size_tokens=512, overlap_tokens=50))
    texts = [c.text for c in chunks]
    t0 = time.perf_counter()
    vectors = embedder.embed_documents(texts)
    wall_ms = (time.perf_counter() - t0) * 1000
    # 单条 query 延迟采样
    q_lats = []
    for sample in texts[:5]:
        t1 = time.perf_counter()
        embedder.embed_query(sample[:200])
        q_lats.append((time.perf_counter() - t1) * 1000)
    return {
        "provider": "DashScope",
        "model": settings.embedding_model,
        "dim": len(vectors[0]) if vectors else 0,
        "chunk_count": len(texts),
        "batch_upsert_ms": round(wall_ms, 1),
        "batch_upsert_ms_reported": round(embedder.last_latency_ms, 1),
        "usage_total_tokens": embedder.last_token_estimate,
        "query_avg_ms": round(statistics.mean(q_lats), 1) if q_lats else 0,
        "query_p95_ms": round(p95(q_lats), 1) if q_lats else 0,
        "cost_estimate_usd": round(
            (embedder.last_token_estimate / 1_000_000) * settings.embedding_price_per_m, 6
        ),
    }


def ensure_index(docs: list[CorpusDoc], size: int, overlap: int) -> VectorStore:
    base = get_settings()
    settings = base.model_copy(
        update={
            "chunk_size_tokens": size,
            "chunk_overlap_tokens": overlap,
            "qdrant_collection": f"geekstack_rag_spike_{size}_{overlap}",
        }
    )
    store = VectorStore(settings)
    store.ensure_collection(recreate=True)
    embedder = get_embedder()
    chunks = []
    for d in docs:
        chunks.extend(chunk_document(d, size_tokens=size, overlap_tokens=overlap))
    vectors = embedder.embed_documents([c.text for c in chunks])
    store.upsert_chunks(chunks, vectors)
    return store


def inspect_schema(store: VectorStore) -> dict:
    """抽检一条 payload，核对统一 schema 字段齐全。"""
    points, _ = store.client.scroll(
        collection_name=store.settings.qdrant_collection,
        limit=1,
        with_payload=True,
        with_vectors=False,
    )
    if not points:
        return {"ok": False, "missing": SCHEMA_REQUIRED, "sample": {}}
    payload = dict(points[0].payload or {})
    missing = [k for k in SCHEMA_REQUIRED if k not in payload]
    sample = {k: payload.get(k) for k in SCHEMA_REQUIRED}
    # text 非空、author_id 已脱敏（非纯数字明文）
    checks = {
        "chunk_text_nonempty": bool(str(payload.get("text") or "").strip()),
        "author_id_masked": str(payload.get("author_id", "")).startswith("u_"),
        "embedding_model": payload.get("embedding_model"),
        "source_system": payload.get("source_system"),
        "content_id_prefix_ok": str(payload.get("content_id", "")).startswith("jkz_"),
    }
    return {
        "ok": not missing
        and checks["chunk_text_nonempty"]
        and checks["author_id_masked"]
        and checks["embedding_model"] == get_settings().embedding_model
        and checks["content_id_prefix_ok"],
        "missing": missing,
        "checks": checks,
        "sample": sample,
    }


def retrieve_context(store: VectorStore, question: str, top_k: int = 5) -> list[str]:
    embedder = get_embedder()
    vec = embedder.embed_query(question)
    hits = store.search(vec, top_k=top_k)
    blocks = []
    for ref, text in hits:
        blocks.append(f"[{ref.title}]({ref.url})\n{text}")
    return blocks


def bench_thinking(store: VectorStore) -> dict:
    llm = MiMoClient()
    questions = [
        "改密码后旧 refresh 是否立即失效？请引用资料说明。",
        "Skill 专区 Java 异常分析助手的用途是什么？",
        "登录失败多少次会触发 429？",
        "MinIO 头像上传有什么大小限制？",
        "社区版规对广告和攻击有什么规定？",
    ]
    modes = ["disabled", "enabled"]
    results = {m: [] for m in modes}
    for mode in modes:
        for q in questions:
            ctx = retrieve_context(store, q)
            out = llm.chat(user_message=q, context_blocks=ctx, thinking=mode)
            answer = out["content"] or ""
            has_url = "http" in answer
            results[mode].append(
                {
                    "question": q,
                    "latency_ms": round(out["latency_ms"], 1),
                    "usage": out["usage"],
                    "answer_preview": answer[:240],
                    "has_source_url": has_url,
                    "has_reasoning": bool(out.get("reasoning_content")),
                    "reasoning_chars": len(out.get("reasoning_content") or ""),
                }
            )
            print(f"[{mode}] {out['latency_ms']:.0f}ms src={has_url} | {q[:32]}")

    summary = {}
    for mode in modes:
        lats = [x["latency_ms"] for x in results[mode]]
        toks_in = sum(x["usage"].get("prompt_tokens", 0) for x in results[mode])
        toks_out = sum(x["usage"].get("completion_tokens", 0) for x in results[mode])
        src_ok = sum(1 for x in results[mode] if x["has_source_url"])
        summary[mode] = {
            "n": len(lats),
            "avg_ms": round(statistics.mean(lats), 1),
            "p95_ms": round(p95(lats), 1),
            "prompt_tokens": toks_in,
            "completion_tokens": toks_out,
            "answers_with_url": src_ok,
        }
    return {"per_call": results, "summary": summary}


def estimate_cost(summary: dict, embedding: dict) -> dict:
    settings = get_settings()
    disabled = summary.get("disabled") or {}
    n = max(1, disabled.get("n") or 1)
    pin = (disabled.get("prompt_tokens") or 0) / n
    pout = (disabled.get("completion_tokens") or 0) / n
    daily_q = 1000
    daily_in = pin * daily_q
    daily_out = pout * daily_q
    llm_daily = (
        daily_in / 1_000_000 * settings.mimo_price_input_per_m
        + daily_out / 1_000_000 * settings.mimo_price_output_per_m
    )
    # 假设每次问答 1 次 query embedding；索引按日增量忽略
    emb_daily = daily_q * (embedding.get("query_avg_ms") and 1)  # noqa: placeholder count
    # 用实测单 query token≈ 粗用 batch tokens/chunks
    tokens_per_chunk = 0
    if embedding.get("chunk_count"):
        tokens_per_chunk = (embedding.get("usage_total_tokens") or 0) / max(
            1, embedding["chunk_count"]
        )
    emb_daily_usd = (daily_q * tokens_per_chunk / 1_000_000) * settings.embedding_price_per_m
    return {
        "assumptions": {
            "model": settings.mimo_model,
            "embedding_model": settings.embedding_model,
            "price_in_per_m_usd": settings.mimo_price_input_per_m,
            "price_out_per_m_usd": settings.mimo_price_output_per_m,
            "embedding_price_per_m_usd": settings.embedding_price_per_m,
            "avg_prompt_tokens_per_ask": round(pin, 1),
            "avg_completion_tokens_per_ask": round(pout, 1),
            "avg_embed_tokens_per_query_est": round(tokens_per_chunk, 1),
            "daily_asks": daily_q,
            "note": "MiMo 价格按套餐粗算 $1/$3 per 1M；embedding 按 $0.1/1M 粗算；以 thinking=disabled 实测为准",
        },
        "llm_daily_usd": round(llm_daily, 4),
        "embedding_daily_usd": round(emb_daily_usd, 4),
        "daily_usd": round(llm_daily + emb_daily_usd, 4),
        "monthly_usd_30d": round((llm_daily + emb_daily_usd) * 30, 2),
        "_unused_emb_daily_flag": emb_daily,
    }


def main() -> None:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    settings = get_settings()
    docs = load_corpus()

    print(
        f"embed={settings.embedding_model} dim={settings.embedding_dim} "
        f"qdrant={settings.qdrant_url} mimo={settings.mimo_model}"
    )

    chunking = bench_chunking(docs)
    print("chunking:", json.dumps(chunking, ensure_ascii=False))

    embedding = bench_embedding(docs)
    print("embedding:", json.dumps(embedding, ensure_ascii=False))

    # 用推荐 512/50 建库做 thinking / 延迟 / schema
    store = ensure_index(docs, 512, 50)
    schema = inspect_schema(store)
    print("schema:", json.dumps(schema, ensure_ascii=False))

    thinking = bench_thinking(store)
    cost = estimate_cost(thinking["summary"], embedding)
    cost.pop("_unused_emb_daily_flag", None)

    from app.graph.rag_graph import build_rag_graph

    graph = build_rag_graph(store=store)
    e2e = []
    for q in [
        "如何绑定 GitHub？",
        "token_version 是干什么的？",
        "项目广场 XSS 怎么防？",
        "登录频控 Redis 挂了会怎样？",
        "Flyway 版本号有什么纪律？",
        "头像上传 scene 是什么？",
        "Skill 版本号用什么格式？",
        "HIDDEN 内容谁能看见？",
    ]:
        t0 = time.perf_counter()
        out = graph.invoke({"question": q, "thinking": "disabled", "top_k": 5})
        e2e.append((time.perf_counter() - t0) * 1000)
        print(f"[e2e] {e2e[-1]:.0f}ms llm={out.get('latency_ms'):.0f} | {q}")

    payload = {
        "run_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "chunking": chunking,
        "embedding": embedding,
        "schema": schema,
        "thinking_compare": thinking["summary"],
        "thinking_samples": {
            "disabled": thinking["per_call"]["disabled"][:2],
            "enabled": thinking["per_call"]["enabled"][:2],
        },
        "cost": cost,
        "latency": {
            "llm_only_disabled_p95_ms": thinking["summary"]["disabled"]["p95_ms"],
            "llm_only_enabled_p95_ms": thinking["summary"]["enabled"]["p95_ms"],
            "e2e_disabled_avg_ms": round(statistics.mean(e2e), 1),
            "e2e_disabled_p95_ms": round(p95(e2e), 1),
            "e2e_samples_ms": [round(x, 1) for x in e2e],
        },
        "decisions_draft": {
            "llm": "mimo-v2.5 @ api.xiaomimimo.com/v1",
            "thinking": "disabled for RAG（见 thinking 对比）",
            "chunk": "512 tokens / 50 overlap",
            "embedding": "DashScope text-embedding-v3 1024-d",
            "vector_db": "Qdrant（社区内容）；管家治理语料留 Milvus",
        },
    }
    out_path = RAW_DIR / "bench_metrics.json"
    out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print("wrote", out_path)


if __name__ == "__main__":
    main()

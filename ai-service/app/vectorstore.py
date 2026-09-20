"""Qdrant 向量库封装（支持 :memory: 与 HTTP）。"""

from __future__ import annotations

from qdrant_client import QdrantClient
from qdrant_client.http import models as qm

from app.config import Settings, get_settings
from app.models import Chunk, SourceRef


class VectorStore:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        url = (self.settings.qdrant_url or "").strip()
        if url in {":memory:", "memory"}:
            self.client = QdrantClient(location=":memory:")
        else:
            self.client = QdrantClient(url=url, timeout=30)

    def ensure_collection(self, recreate: bool = False) -> None:
        name = self.settings.qdrant_collection
        exists = self.client.collection_exists(name)
        if exists and recreate:
            self.client.delete_collection(name)
            exists = False
        if not exists:
            self.client.create_collection(
                collection_name=name,
                vectors_config=qm.VectorParams(
                    size=self.settings.embedding_dim,
                    distance=qm.Distance.COSINE,
                ),
            )

    def upsert_chunks(self, chunks: list[Chunk], vectors: list[list[float]]) -> int:
        if not chunks:
            return 0
        points = []
        for chunk, vec in zip(chunks, vectors):
            # 统一 metadata schema（对齐管家草案 + chunk 文本入 payload）
            payload = {
                "content_id": chunk.chunk_id,
                "parent_content_id": chunk.content_id,
                "source_system": chunk.source_system or self.settings.source_system,
                "content_type": chunk.content_type,
                "text_url": chunk.url,
                "author_id": _mask_author(chunk.author_id),
                "created_at": chunk.created_at or "",
                "visibility": chunk.visibility or "public",
                "embedding_model": self.settings.embedding_model,
                "chunk_id": chunk.chunk_id,
                "chunk_index": chunk.chunk_index,
                "title": chunk.title,
                "chunk_text": chunk.text,
                "text": chunk.text,  # 与 chunk_text 同值（schema v1.1 硬约束）
                "extra": {},
            }
            points.append(
                qm.PointStruct(
                    id=abs(hash(chunk.chunk_id)) % (2**63 - 1),
                    vector=vec,
                    payload=payload,
                )
            )
        self.client.upsert(collection_name=self.settings.qdrant_collection, points=points)
        return len(points)

    def search(self, query_vector: list[float], top_k: int) -> list[tuple[SourceRef, str]]:
        hits = self.client.search(
            collection_name=self.settings.qdrant_collection,
            query_vector=query_vector,
            limit=top_k,
            with_payload=True,
        )
        best: dict[str, tuple[SourceRef, str]] = {}
        for hit in hits:
            payload = hit.payload or {}
            parent = str(payload.get("parent_content_id") or payload.get("content_id") or "")
            ref = SourceRef(
                content_id=parent,
                title=str(payload.get("title", "")),
                url=str(payload.get("text_url") or payload.get("url", "")),
                score=float(hit.score or 0.0),
            )
            text = str(payload.get("chunk_text") or payload.get("text") or "")
            prev = best.get(parent)
            if prev is None or ref.score > prev[0].score:
                best[parent] = (ref, text)
        return sorted(best.values(), key=lambda x: x[0].score, reverse=True)


def _mask_author(author_id: int) -> str:
    """脱敏：不落用户名/邮箱，仅保留不可逆短哈希前缀 + 原 id 占位策略用哈希。"""
    digest = abs(hash(f"author:{author_id}")) % 10_000_000
    return f"u_{digest:07d}"

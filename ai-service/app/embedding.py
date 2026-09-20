"""DashScope text-embedding-v3（OpenAI 兼容接口，1024 维）。"""

from __future__ import annotations

import logging
import time
from functools import lru_cache

from openai import OpenAI

from app.config import Settings, get_settings

log = logging.getLogger(__name__)


class Embedder:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        if not self.settings.dashscope_api_key:
            raise RuntimeError("DASHSCOPE_API_KEY 未配置（请写入仓库根目录 .env）")
        self.client = OpenAI(
            api_key=self.settings.dashscope_api_key,
            base_url=self.settings.dashscope_base_url,
            timeout=60.0,
        )
        self.last_latency_ms: float = 0.0
        self.last_token_estimate: int = 0

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        batch_size = max(1, min(self.settings.embedding_batch_size, 10))
        vectors: list[list[float]] = []
        t0 = time.perf_counter()
        for i in range(0, len(texts), batch_size):
            batch = texts[i : i + batch_size]
            resp = self.client.embeddings.create(
                model=self.settings.embedding_model,
                input=batch,
                dimensions=self.settings.embedding_dim,
                encoding_format="float",
            )
            ordered = sorted(resp.data, key=lambda d: d.index)
            vectors.extend([list(d.embedding) for d in ordered])
            usage = getattr(resp, "usage", None)
            if usage is not None:
                self.last_token_estimate += int(getattr(usage, "total_tokens", 0) or 0)
        self.last_latency_ms = (time.perf_counter() - t0) * 1000
        if len(vectors) != len(texts):
            raise RuntimeError(f"embedding 条数不符: got {len(vectors)} expect {len(texts)}")
        if vectors and len(vectors[0]) != self.settings.embedding_dim:
            raise RuntimeError(
                f"embedding 维度不符: got {len(vectors[0])} expect {self.settings.embedding_dim}"
            )
        return vectors

    def embed_query(self, text: str) -> list[float]:
        return self.embed_documents([text])[0]


@lru_cache
def get_embedder() -> Embedder:
    return Embedder()

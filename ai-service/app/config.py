from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# 仓库根 .env + ai-service/.env（后者覆盖）
_ROOT = Path(__file__).resolve().parents[2]
_AI = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=(str(_ROOT / ".env"), str(_AI / ".env")),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    mimo_api_key: str = ""
    mimo_base_url: str = "https://api.xiaomimimo.com/v1"
    mimo_model: str = "mimo-v2.5"
    mimo_thinking: str = "disabled"  # enabled | disabled

    # ":memory:" = 本地内存库（无 Docker 时 spike/bench）；否则 HTTP URL
    qdrant_url: str = ":memory:"
    qdrant_collection: str = "geekstack_rag_spike"

    dashscope_api_key: str = ""
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    embedding_model: str = "text-embedding-v3"
    embedding_dim: int = 1024
    embedding_batch_size: int = 10  # DashScope 单批上限

    chunk_size_tokens: int = 512
    chunk_overlap_tokens: int = 50
    retrieve_top_k: int = 5
    # Cosine 相似度下限；低于此视为未命中（超阈值分支）
    retrieve_score_threshold: float = 0.35

    source_system: str = "geekstack"

    # 成本估算用（美元 / 1M tokens；以官方套餐粗算，报告中注明）
    mimo_price_input_per_m: float = 1.0
    mimo_price_output_per_m: float = 3.0
    # DashScope text-embedding-v3 国内价约 ¥0.0007/千 tokens ≈ $0.1/1M（粗算）
    embedding_price_per_m: float = 0.1


@lru_cache
def get_settings() -> Settings:
    return Settings()

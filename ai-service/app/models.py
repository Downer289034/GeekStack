from typing import Any

from pydantic import BaseModel, Field


class CorpusDoc(BaseModel):
    content_id: str
    content_type: str  # post | share | skill | help
    title: str
    url: str
    author_id: int = 0
    body_md: str
    created_at: str = ""


class Chunk(BaseModel):
    chunk_id: str
    content_id: str
    content_type: str
    title: str
    url: str
    author_id: int
    text: str
    chunk_index: int
    source_system: str = "geekstack"
    created_at: str = ""
    visibility: str = "public"


class SourceRef(BaseModel):
    content_id: str
    title: str
    url: str
    score: float


class AskRequest(BaseModel):
    question: str = Field(min_length=1, max_length=2000)
    thinking: str | None = None  # override: enabled|disabled
    top_k: int | None = None


class AskResponse(BaseModel):
    answer: str
    sources: list[SourceRef]
    latency_ms: float
    usage: dict[str, Any] = Field(default_factory=dict)
    thinking: str = "disabled"

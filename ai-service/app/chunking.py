"""Markdown 粗清洗 + token 窗口分块（tiktoken cl100k_base 近似）。"""

from __future__ import annotations

import re

import tiktoken

from app.models import Chunk, CorpusDoc

_ENC = tiktoken.get_encoding("cl100k_base")
_MD_STRIP = re.compile(
    r"```[\s\S]*?```|`[^`]+`|!\[[^\]]*\]\([^)]+\)|\[[^\]]*\]\([^)]+\)|[#>*_\-]{1,}"
)


def strip_markdown(md: str) -> str:
    text = _MD_STRIP.sub(" ", md or "")
    text = re.sub(r"\s+", " ", text).strip()
    return text


def count_tokens(text: str) -> int:
    return len(_ENC.encode(text or ""))


def chunk_document(
    doc: CorpusDoc,
    size_tokens: int = 512,
    overlap_tokens: int = 50,
) -> list[Chunk]:
    plain = strip_markdown(doc.body_md)
    if not plain:
        plain = doc.title
    tokens = _ENC.encode(plain)
    if not tokens:
        return []

    step = max(1, size_tokens - overlap_tokens)
    chunks: list[Chunk] = []
    index = 0
    start = 0
    while start < len(tokens):
        end = min(len(tokens), start + size_tokens)
        piece = _ENC.decode(tokens[start:end]).strip()
        if piece:
            chunks.append(
                Chunk(
                    chunk_id=f"{doc.content_id}#{index}",
                    content_id=doc.content_id,
                    content_type=doc.content_type,
                    title=doc.title,
                    url=doc.url,
                    author_id=doc.author_id,
                    text=piece,
                    chunk_index=index,
                    source_system="geekstack",
                    created_at=doc.created_at or "",
                    visibility="public",
                )
            )
            index += 1
        if end >= len(tokens):
            break
        start += step
    return chunks

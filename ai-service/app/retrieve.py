"""检索分支判定（T-AI-2）：空结果 / 正常命中 / 全低于阈值。"""

from __future__ import annotations

from app.models import SourceRef

FALLBACK_ANSWER = "根据现有资料无法确定。请换个问法，或先浏览论坛/Skill 广场。"

BRANCH_EMPTY = "empty"
BRANCH_BELOW = "below_threshold"
BRANCH_OK = "ok"


def classify_retrieval(
    hits: list[tuple[SourceRef, str]],
    score_threshold: float,
) -> tuple[str, list[tuple[SourceRef, str]]]:
    """返回 (分支, 保留的命中)。

    - empty：检索结果为空
    - below_threshold：有结果但全部分数低于阈值
    - ok：至少一条达到阈值
    """
    if not hits:
        return BRANCH_EMPTY, []
    kept = [(ref, text) for ref, text in hits if ref.score >= score_threshold]
    if not kept:
        return BRANCH_BELOW, []
    return BRANCH_OK, kept

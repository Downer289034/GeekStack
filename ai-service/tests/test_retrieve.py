"""T-AI-2：检索三分支 + SSE 兜底不调用 LLM。"""

from __future__ import annotations

import unittest

from app.graph.rag_graph import iter_answer_events
from app.models import SourceRef
from app.retrieve import BRANCH_BELOW, BRANCH_EMPTY, BRANCH_OK, FALLBACK_ANSWER, classify_retrieval


def _hit(score: float, title: str = "t") -> tuple[SourceRef, str]:
    return (
        SourceRef(content_id="jkz_post_1", title=title, url="https://geekstack.local/posts/1", score=score),
        "正文",
    )


class ClassifyRetrievalTest(unittest.TestCase):
    def test_empty(self):
        branch, kept = classify_retrieval([], 0.35)
        self.assertEqual(branch, BRANCH_EMPTY)
        self.assertEqual(kept, [])

    def test_normal_keeps_hits_at_or_above_threshold(self):
        hits = [_hit(0.82, "命中"), _hit(0.2, "丢弃")]
        branch, kept = classify_retrieval(hits, 0.35)
        self.assertEqual(branch, BRANCH_OK)
        self.assertEqual(len(kept), 1)
        self.assertEqual(kept[0][0].title, "命中")

    def test_all_below_threshold(self):
        branch, kept = classify_retrieval([_hit(0.11), _hit(0.34)], 0.35)
        self.assertEqual(branch, BRANCH_BELOW)
        self.assertEqual(kept, [])


class StreamFallbackTest(unittest.TestCase):
    def test_below_threshold_emits_fallback_without_llm(self):
        events = list(
            iter_answer_events(
                {"branch": BRANCH_BELOW, "contexts": [], "sources": [], "question": "无关问题"},
                llm=None,
            )
        )
        self.assertEqual(events[0]["event"], "token")
        self.assertEqual(events[0]["data"]["delta"], FALLBACK_ANSWER)
        self.assertEqual(events[1]["data"]["sources"], [])
        self.assertEqual(events[2]["data"]["branch"], BRANCH_BELOW)


if __name__ == "__main__":
    unittest.main()

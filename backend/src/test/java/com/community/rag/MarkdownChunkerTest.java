package com.community.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.community.rag.model.RagPoint;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarkdownChunkerTest {

    @Test
    void chunksRespectWindowAndStripMarkdown() {
        RagProperties props = new RagProperties();
        props.setChunkSizeTokens(8);
        props.setChunkOverlapTokens(2);
        MarkdownChunker chunker = new MarkdownChunker(props);
        List<String> chunks = chunker.chunk("## 标题\n\n一二三四五六七八九十十一十二");
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0)).doesNotContain("##");
    }

    @Test
    void schemaV11PayloadContainsRequiredFields() {
        Map<String, Object> payload = RagPoint.buildPayload(
                "jkz_post_1",
                "geekstack",
                "post",
                "http://localhost/posts/1",
                "u_0000001",
                "2026-09-20T00:00:00",
                "public",
                "text-embedding-v3",
                "title",
                0,
                "chunk body",
                Map.of());
        assertThat(payload).containsKeys(
                "content_id", "parent_content_id", "source_system", "content_type",
                "text_url", "author_id", "created_at", "visibility", "embedding_model",
                "chunk_index", "chunk_text", "text", "extra");
        assertThat(payload.get("text")).isEqualTo(payload.get("chunk_text"));
        assertThat(payload.get("parent_content_id")).isEqualTo("jkz_post_1");
        assertThat(payload.get("content_id")).isEqualTo("jkz_post_1#0");
        assertThat(payload.get("chunk_index")).isEqualTo(0);
    }
}

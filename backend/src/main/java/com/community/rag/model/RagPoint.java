package com.community.rag.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * schema v1.1 向量点：统一字段 + chunk_text/chunk_index/parent_content_id + text 硬约束。
 */
public record RagPoint(
        String pointId,
        List<Float> vector,
        Map<String, Object> payload) {

    public static Map<String, Object> buildPayload(
            String parentContentId,
            String sourceSystem,
            String contentType,
            String textUrl,
            String authorIdMasked,
            String createdAt,
            String visibility,
            String embeddingModel,
            String title,
            int chunkIndex,
            String chunkText,
            Map<String, Object> extra) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("content_id", parentContentId + "#" + chunkIndex);
        p.put("parent_content_id", parentContentId);
        p.put("source_system", sourceSystem);
        p.put("content_type", contentType);
        p.put("text_url", textUrl);
        p.put("author_id", authorIdMasked);
        p.put("created_at", createdAt == null ? "" : createdAt);
        p.put("visibility", visibility);
        p.put("embedding_model", embeddingModel);
        p.put("title", title == null ? "" : title);
        p.put("chunk_index", chunkIndex);
        p.put("chunk_text", chunkText);
        p.put("text", chunkText); // 硬约束：与 chunk_text 同值，兼容检索侧读 text
        p.put("extra", extra == null ? Map.of() : extra);
        return p;
    }
}

package com.community.rag.model;

import java.util.Map;

/**
 * 待索引文档（入库前）。
 */
public record RagDocument(
        String parentContentId,
        String contentType,
        String title,
        String textUrl,
        long authorId,
        String createdAt,
        String visibility,
        String bodyMd,
        Map<String, Object> extra) {
}

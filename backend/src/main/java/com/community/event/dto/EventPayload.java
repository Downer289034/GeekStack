package com.community.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Agent 冻结的 Event Schema（community-api-contract.md §3）。
 * 极客栈侧 webhook 请求体直接使用此 DTO，不做二次包装。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventPayload(
        String contentId,
        String contentType,
        String contentText,
        String authorId,
        Metadata metadata
) {
    public record Metadata(
            String source,
            String action,
            String eventTime,
            String targetType,
            Boolean truncated
    ) {
        public static Metadata created(String targetType, boolean truncated) {
            return new Metadata(
                    "jikezhan",
                    "created",
                    OffsetDateTime.now().toString(),
                    targetType,
                    truncated);
        }
    }
}

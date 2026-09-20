package com.community.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.OffsetDateTime;

/**
 * Agent 冻结的 Event Schema（community-api-contract.md §3）。
 * 极客栈侧 webhook 请求体直接使用此 DTO，不做二次包装。
 * JSON 字段为 snake_case（content_id / event_time 等），与契约一致。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EventPayload(
        String contentId,
        String contentType,
        String contentText,
        String authorId,
        Metadata metadata
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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

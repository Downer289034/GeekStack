package com.community.share.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目广场卡片响应（不含正文，列表轻量化）。
 */
public record ShareCardResponse(
        Long id,
        String title,
        String summary,
        List<String> tags,
        String repoUrl,
        Long viewCount,
        LocalDateTime createdAt
) {
}

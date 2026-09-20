package com.community.steward.dto;

import java.time.LocalDateTime;

/**
 * 用户内容历史单条（脱敏）。不含邮箱 / Token / 密文。
 */
public record StewardContentItem(
        String contentId,
        String userId,
        String username,
        String contentType,
        String title,
        String textSnippet,
        String targetId,
        LocalDateTime createdAt
) {
}

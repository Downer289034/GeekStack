package com.community.forum.dto;

import java.time.LocalDateTime;

/**
 * 帖子列表卡片（不含正文）。
 */
public record PostCardResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        Long viewCount,
        Long replyCount,
        LocalDateTime lastReplyAt,
        Integer isPinned,
        Integer isHot,
        LocalDateTime createdAt
) {
}

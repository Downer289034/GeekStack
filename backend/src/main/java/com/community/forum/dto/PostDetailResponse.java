package com.community.forum.dto;

import java.time.LocalDateTime;

/**
 * 帖子详情响应（含正文）。
 */
public record PostDetailResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        String contentMd,
        Long viewCount,
        Long replyCount,
        LocalDateTime lastReplyAt,
        Integer isPinned,
        Integer isHot,
        LocalDateTime createdAt
) {
}

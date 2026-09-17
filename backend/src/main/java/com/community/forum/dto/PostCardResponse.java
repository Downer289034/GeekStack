package com.community.forum.dto;

import com.community.forum.entity.Post;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子列表卡片（不含正文）。
 * T21.1：from(Post) 为唯一转换入口，消除 PostService 与 HotPostService 的重复 toCard。
 */
public record PostCardResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        List<String> tags,
        Long viewCount,
        Long replyCount,
        LocalDateTime lastReplyAt,
        Integer isPinned,
        Integer isHot,
        LocalDateTime createdAt
) {
    /** 唯一转换入口（T21.1 toCard 收口）。 */
    public static PostCardResponse from(Post post) {
        List<String> tags = post.getTags() == null ? List.of() : List.copyOf(post.getTags());
        return new PostCardResponse(post.getId(), post.getUserId(), post.getCategoryId(),
                post.getTitle(), tags, post.getViewCount(), post.getReplyCount(),
                post.getLastReplyAt(), post.getIsPinned(), post.getIsHot(), post.getCreatedAt());
    }
}

package com.community.forum.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回帖树节点：顶级回帖携带楼中楼 children（仅一级嵌套）。
 */
public record CommentResponse(
        Long id,
        Long userId,
        Long parentId,
        String contentMd,
        Long likeCount,
        LocalDateTime createdAt,
        List<CommentResponse> children
) {
}

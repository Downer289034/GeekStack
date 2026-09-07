package com.community.forum.dto;

/**
 * 分类响应。
 */
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Integer sortOrder
) {
}

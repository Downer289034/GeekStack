package com.community.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建帖子请求。
 */
public record CreatePostRequest(
        @NotNull
        Long categoryId,

        @NotBlank @Size(max = 200)
        String title,

        @NotBlank
        String contentMd
) {
}

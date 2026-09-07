package com.community.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 编辑帖子请求（分类不可改）。
 */
public record UpdatePostRequest(
        @NotBlank @Size(max = 200)
        String title,

        @NotBlank
        String contentMd
) {
}

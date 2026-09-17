package com.community.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建帖子请求。tags 可选，服务端归一化后最多 5 个。
 */
public record CreatePostRequest(
        @NotNull
        Long categoryId,

        @NotBlank @Size(max = 200)
        String title,

        @NotBlank
        String contentMd,

        @Size(max = 5, message = "标签最多 5 个")
        List<String> tags
) {
}

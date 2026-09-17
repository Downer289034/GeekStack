package com.community.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 编辑帖子请求（分类不可改）。tags 为 null 表示不改；空数组可清空。
 */
public record UpdatePostRequest(
        @NotBlank @Size(max = 200)
        String title,

        @NotBlank
        String contentMd,

        @Size(max = 5, message = "标签最多 5 个")
        List<String> tags
) {
}

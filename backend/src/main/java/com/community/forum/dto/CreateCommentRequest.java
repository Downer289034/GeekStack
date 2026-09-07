package com.community.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建回帖请求。宿主由路径决定（帖子或项目分享）；parentId 为空 = 顶级回帖，
 * 非空 = 楼中楼（仅允许一级嵌套，父评论必须是顶级）。
 */
public record CreateCommentRequest(
        @NotBlank @Size(max = 5000)
        String contentMd,

        Long parentId
) {
}

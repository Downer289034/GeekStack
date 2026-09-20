package com.community.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 管理员设置帖子置顶：0 取消 / 1 置顶。
 */
public record UpdatePostPinnedRequest(
        @NotNull @Min(0) @Max(1) Integer pinned) {
}

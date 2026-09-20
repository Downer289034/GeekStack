package com.community.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 管理员设置帖子加精：0 取消 / 1 加精。
 */
public record UpdatePostFeaturedRequest(
        @NotNull @Min(0) @Max(1) Integer featured) {
}

package com.community.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理员更新用户状态：0 正常 / 1 封禁；reason 可选（审计用）。
 */
public record UpdateUserStatusRequest(
        @NotNull @Min(0) @Max(1) Integer status,
        @Size(max = 500) String reason) {
}

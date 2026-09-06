package com.community.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求（用户名 + 密码）。
 */
public record LoginRequest(
        @NotBlank
        String username,

        @NotBlank
        String password
) {
}

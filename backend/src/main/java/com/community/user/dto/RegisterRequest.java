package com.community.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "只能包含字母、数字与下划线")
        String username,

        @NotBlank @Email @Size(max = 100)
        String email,

        @NotBlank @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
        String password
) {
}

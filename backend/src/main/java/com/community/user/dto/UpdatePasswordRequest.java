package com.community.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 设置/修改密码。无旧密码（OAuth 首设）时 oldPassword 可空；已有密码必须验旧。
 */
public record UpdatePasswordRequest(
        String oldPassword,

        @NotBlank @Size(min = 8, max = 64)
        String newPassword
) {
}

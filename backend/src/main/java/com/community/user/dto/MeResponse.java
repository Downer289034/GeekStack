package com.community.user.dto;

import java.time.LocalDateTime;

/**
 * 当前登录用户资料（含是否已设密码与 OAuth 绑定态）。
 */
public record MeResponse(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        LocalDateTime createdAt,
        boolean hasPassword,
        OAuthBindingsResponse oauthBindings
) {
}

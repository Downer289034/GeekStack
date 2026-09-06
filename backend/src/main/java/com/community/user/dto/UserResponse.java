package com.community.user.dto;

import java.time.LocalDateTime;

/**
 * 用户公开信息（me / 主页聚合复用）。
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        LocalDateTime createdAt
) {
}

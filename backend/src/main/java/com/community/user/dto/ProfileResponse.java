package com.community.user.dto;

import java.time.LocalDateTime;

/**
 * 主页资料卡（公开字段，不含邮箱等隐私）。
 */
public record ProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        LocalDateTime createdAt
) {
}

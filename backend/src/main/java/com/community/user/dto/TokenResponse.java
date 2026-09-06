package com.community.user.dto;

/**
 * 令牌对响应。
 */
public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}

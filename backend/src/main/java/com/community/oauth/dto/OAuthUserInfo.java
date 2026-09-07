package com.community.oauth.dto;

/**
 * 平台侧用户信息（统一抽象：GitHub/Gitee 拉取后归一化）。
 */
public record OAuthUserInfo(
        String openid,
        String username,
        String nickname,
        String email,
        String avatarUrl
) {
}

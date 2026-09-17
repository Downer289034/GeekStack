package com.community.user.dto;

/**
 * 当前用户 OAuth 绑定状态。
 */
public record OAuthBindingsResponse(boolean github, boolean gitee) {
}

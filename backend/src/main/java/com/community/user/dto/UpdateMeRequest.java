package com.community.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新当前用户资料（部分字段；null 表示不改）。
 */
public record UpdateMeRequest(
        @Size(max = 50)
        String nickname,

        @Size(max = 500)
        String bio,

        @Size(max = 500)
        String avatarUrl
) {
}

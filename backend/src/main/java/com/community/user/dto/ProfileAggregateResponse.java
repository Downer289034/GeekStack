package com.community.user.dto;

import com.community.forum.dto.PostCardResponse;
import com.community.share.dto.ShareResponse;
import java.util.List;

/**
 * 个人主页聚合：资料 + 项目分享 + 帖子（各限量，见 ProfileService）。
 * shares 的可见性：他人视角仅 PUBLISHED；本人视角含 HIDDEN。
 */
public record ProfileAggregateResponse(
        ProfileResponse profile,
        List<ShareResponse> shares,
        List<PostCardResponse> posts
) {
}

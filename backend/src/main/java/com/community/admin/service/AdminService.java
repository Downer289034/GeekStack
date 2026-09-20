package com.community.admin.service;

import com.community.admin.dto.UpdatePostFeaturedRequest;
import com.community.admin.dto.UpdatePostPinnedRequest;
import com.community.admin.dto.UpdateUserStatusRequest;
import com.community.forum.entity.Post;
import com.community.forum.service.PostService;
import com.community.user.dto.UserResponse;
import com.community.user.entity.User;
import com.community.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 管理端处置：删帖 / 禁言 / 加精 / 置顶，并落结构化审计日志（供 Agent 处置可回放）。
 */
@Service
public class AdminService {

    /** 专用审计通道：grep ADMIN_AUDIT 即可回放处置动作。 */
    private static final Logger AUDIT = LoggerFactory.getLogger("ADMIN_AUDIT");

    private final PostService postService;
    private final UserService userService;

    public AdminService(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    /** 管理员删帖（软删口径与作者自删一致），记录操作者。 */
    public void deletePost(long postId, String adminUsername) {
        UserResponse admin = userService.me(adminUsername);
        Post post = postService.deleteByAdmin(postId);
        AUDIT.info(
                "action=ADMIN_DELETE_POST operatorId={} operator={} targetPostId={} targetUserId={}",
                admin.id(), admin.username(), post.getId(), post.getUserId());
    }

    /** 禁言/解封（0/1），记录操作者与可选原因。 */
    public void updateUserStatus(long userId, UpdateUserStatusRequest request, String adminUsername) {
        UserResponse admin = userService.me(adminUsername);
        User target = userService.updateStatus(userId, request.status());
        String reason = request.reason() == null ? "" : request.reason().trim();
        AUDIT.info(
                "action=ADMIN_SET_USER_STATUS operatorId={} operator={} targetUserId={} status={} reason={}",
                admin.id(), admin.username(), target.getId(), target.getStatus(), reason);
    }

    /** 加精/取消（0/1，幂等），operator 可区分（含 steward_bot）。 */
    public void updatePostFeatured(long postId, UpdatePostFeaturedRequest request, String adminUsername) {
        UserResponse admin = userService.me(adminUsername);
        Post post = postService.setFeaturedByAdmin(postId, request.featured());
        AUDIT.info(
                "action=ADMIN_SET_POST_FEATURED operatorId={} operator={} targetPostId={} targetUserId={} featured={}",
                admin.id(), admin.username(), post.getId(), post.getUserId(), post.getIsFeatured());
    }

    /** 置顶/取消（0/1，幂等），operator 可区分（含 steward_bot）。 */
    public void updatePostPinned(long postId, UpdatePostPinnedRequest request, String adminUsername) {
        UserResponse admin = userService.me(adminUsername);
        Post post = postService.setPinnedByAdmin(postId, request.pinned());
        AUDIT.info(
                "action=ADMIN_SET_POST_PINNED operatorId={} operator={} targetPostId={} targetUserId={} pinned={}",
                admin.id(), admin.username(), post.getId(), post.getUserId(), post.getIsPinned());
    }
}

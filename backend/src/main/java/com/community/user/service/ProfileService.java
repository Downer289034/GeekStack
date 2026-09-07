package com.community.user.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.forum.dto.PostCardResponse;
import com.community.forum.service.PostService;
import com.community.share.dto.ShareResponse;
import com.community.share.service.ShareService;
import com.community.user.dto.ProfileAggregateResponse;
import com.community.user.dto.ProfileResponse;
import com.community.user.dto.UserResponse;
import com.community.user.entity.User;
import com.community.user.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 个人主页聚合（user 模块拥有入口，反向依赖 share/forum 的 Service 查询接口——架构允许的方向）。
 * 可见性口径与详情页一致：HIDDEN 分享仅作者本人可见（登录名 = 主页用户名时 owner=true）。
 * 列表限量：主页不是无限流，分享/帖子各取前 10 条。
 */
@Service
public class ProfileService {

    static final int PROFILE_LIST_LIMIT = 10;

    private final UserMapper userMapper;
    private final ShareService shareService;
    private final PostService postService;

    public ProfileService(UserMapper userMapper, ShareService shareService, PostService postService) {
        this.userMapper = userMapper;
        this.shareService = shareService;
        this.postService = postService;
    }

    /**
     * 按用户名聚合主页数据；用户不存在返回 404（前端进全局 404 页）。
     *
     * @param viewerUsername 当前登录用户名，未登录传 null
     */
    public ProfileAggregateResponse profile(String username, String viewerUsername) {
        User user = userMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        boolean owner = viewerUsername != null && viewerUsername.equals(username);
        ProfileResponse profile = new ProfileResponse(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatarUrl(), user.getBio(), user.getRole(), user.getCreatedAt());
        List<ShareResponse> shares = shareService.listByUser(user.getId(), owner, PROFILE_LIST_LIMIT);
        List<PostCardResponse> posts = postService.listByUser(user.getId(), PROFILE_LIST_LIMIT);
        return new ProfileAggregateResponse(profile, shares, posts);
    }

    /** 供 me 等场景复用的公开资料视图。 */
    public ProfileResponse toProfile(UserResponse user) {
        return new ProfileResponse(user.id(), user.username(), user.nickname(), user.avatarUrl(),
                user.bio(), user.role(), user.createdAt());
    }
}

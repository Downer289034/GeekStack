package com.community.share.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.share.dto.SharePayload;
import com.community.share.dto.ShareResponse;
import com.community.share.entity.ProjectShare;
import com.community.share.mapper.ProjectShareMapper;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 项目分享：发布 / 编辑 / 下架。编辑与下架仅限作者本人（越权返回 403）。
 * 跨模块仅调用 user 模块 Service 接口获取用户信息，不触碰其 Mapper。
 */
@Service
public class ShareService {

    private final ProjectShareMapper shareMapper;
    private final UserService userService;

    public ShareService(ProjectShareMapper shareMapper, UserService userService) {
        this.shareMapper = shareMapper;
        this.userService = userService;
    }

    /** 发布项目分享。 */
    public ShareResponse create(String username, SharePayload payload) {
        long userId = requireUserId(username);
        ProjectShare share = new ProjectShare();
        share.setUserId(userId);
        applyPayload(share, payload);
        share.setStatus("PUBLISHED");
        shareMapper.insert(share);
        return toResponse(share);
    }

    /** 编辑项目分享，仅限作者本人。 */
    public ShareResponse update(long id, String username, SharePayload payload) {
        ProjectShare share = requireOwned(id, username);
        applyPayload(share, payload);
        shareMapper.updateById(share);
        return toResponse(share);
    }

    /** 下架项目分享（status=HIDDEN），仅限作者本人。 */
    public ShareResponse hide(long id, String username) {
        ProjectShare share = requireOwned(id, username);
        share.setStatus("HIDDEN");
        shareMapper.updateById(share);
        return toResponse(share);
    }

    private void applyPayload(ProjectShare share, SharePayload payload) {
        share.setTitle(payload.title());
        share.setSummary(payload.summary());
        share.setContentMd(payload.contentMd());
        share.setRepoUrl(payload.repoUrl());
        share.setDemoUrl(payload.demoUrl());
        share.setTags(payload.tags());
    }

    private ProjectShare requireOwned(long id, String username) {
        ProjectShare share = shareMapper.selectById(id);
        if (share == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目分享不存在");
        }
        if (!share.getUserId().equals(requireUserId(username))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅限作者本人操作");
        }
        return share;
    }

    private long requireUserId(String username) {
        UserResponse user = userService.me(username);
        return user.id();
    }

    private ShareResponse toResponse(ProjectShare share) {
        return new ShareResponse(share.getId(), share.getUserId(), share.getTitle(), share.getSummary(),
                share.getContentMd(), share.getRepoUrl(), share.getDemoUrl(), share.getTags(),
                share.getStatus(), share.getViewCount(), share.getCreatedAt());
    }
}

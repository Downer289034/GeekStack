package com.community.share.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.PageResponse;
import com.community.event.ContentCreatedEvent;
import com.community.rag.event.RagContentChangedEvent;
import com.community.share.dto.ShareCardResponse;
import com.community.share.dto.SharePayload;
import com.community.share.dto.ShareResponse;
import com.community.share.entity.ProjectShare;
import com.community.share.mapper.ProjectShareMapper;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 项目分享：发布 / 编辑 / 下架 / 广场分页 / 详情。编辑与下架仅限作者本人（越权返回 403）。
 * 跨模块仅调用 user 模块 Service 接口获取用户信息，不触碰其 Mapper。
 */
@Service
public class ShareService {

    private final ProjectShareMapper shareMapper;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public ShareService(ProjectShareMapper shareMapper, UserService userService,
                        ApplicationEventPublisher eventPublisher) {
        this.shareMapper = shareMapper;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    /** 发布项目分享。 */
    public ShareResponse create(String username, SharePayload payload) {
        long userId = requireUserId(username);
        ProjectShare share = new ProjectShare();
        share.setUserId(userId);
        applyPayload(share, payload);
        share.setStatus("PUBLISHED");
        shareMapper.insert(share);
        // T14.6：发布 share.created 事件（content_text = 标题+摘要，截断由 Listener 处理）
        String text = share.getTitle() + "\n" + share.getSummary();
        eventPublisher.publishEvent(new ContentCreatedEvent.ShareCreated(this, share.getId(), text, userId));
        eventPublisher.publishEvent(RagContentChangedEvent.upsert(this, "share", share.getId()));
        return toResponse(share);
    }

    /** 编辑项目分享，仅限作者本人。 */
    public ShareResponse update(long id, String username, SharePayload payload) {
        ProjectShare share = requireOwned(id, username);
        applyPayload(share, payload);
        shareMapper.updateById(share);
        eventPublisher.publishEvent(RagContentChangedEvent.upsert(this, "share", share.getId()));
        return toResponse(share);
    }

    /** 下架项目分享（status=HIDDEN），仅限作者本人。 */
    public ShareResponse hide(long id, String username) {
        ProjectShare share = requireOwned(id, username);
        share.setStatus("HIDDEN");
        shareMapper.updateById(share);
        eventPublisher.publishEvent(RagContentChangedEvent.delete(this, "share", share.getId()));
        return toResponse(share);
    }

    /** 广场分页：仅 PUBLISHED，按创建时间倒序，可选单个标签筛选（JSON_CONTAINS 参数化，无注入）。 */
    public PageResponse<ShareCardResponse> pagePublished(long current, long size, String tag) {
        LambdaQueryWrapper<ProjectShare> wrapper = Wrappers.<ProjectShare>lambdaQuery()
                .eq(ProjectShare::getStatus, "PUBLISHED")
                // created_at 秒级精度，同秒并列时用自增 id 保证稳定排序
                .orderByDesc(ProjectShare::getCreatedAt, ProjectShare::getId);
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag.trim());
        }
        Page<ProjectShare> page = shareMapper.selectPage(Page.of(current, size), wrapper);
        return PageResponse.from(page.convert(this::toCard));
    }

    /**
     * 详情：浏览数原子 +1；HIDDEN 状态仅作者本人可见（他人/未登录视为不存在）。
     *
     * @param username 当前登录用户名，未登录传 null
     */
    public ShareResponse getDetail(long id, String username) {
        ProjectShare share = shareMapper.selectById(id);
        if (share == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目分享不存在");
        }
        boolean isOwner = username != null && share.getUserId().equals(requireUserId(username));
        if ("HIDDEN".equals(share.getStatus()) && !isOwner) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目分享不存在");
        }
        shareMapper.update(null, Wrappers.<ProjectShare>lambdaUpdate()
                .eq(ProjectShare::getId, id)
                .setSql("view_count = view_count + 1"));
        share.setViewCount(share.getViewCount() == null ? 1 : share.getViewCount() + 1);
        return toResponse(share);
    }

    /**
     * 某用户的项目分享（主页聚合用，限量取前 N 条）。
     * 可见性与详情页一致：PUBLISHED 对所有人可见；HIDDEN 仅作者本人可见（owner=true 时包含）。
     */
    public List<ShareResponse> listByUser(long userId, boolean owner, int limit) {
        LambdaQueryWrapper<ProjectShare> wrapper = Wrappers.<ProjectShare>lambdaQuery()
                .eq(ProjectShare::getUserId, userId)
                .orderByDesc(ProjectShare::getCreatedAt, ProjectShare::getId);
        if (!owner) {
            wrapper.eq(ProjectShare::getStatus, "PUBLISHED");
        }
        wrapper.last("LIMIT " + limit);
        return shareMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    private ShareCardResponse toCard(ProjectShare share) {
        return new ShareCardResponse(share.getId(), share.getTitle(), share.getSummary(), share.getTags(),
                share.getRepoUrl(), share.getViewCount(), share.getCreatedAt());
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

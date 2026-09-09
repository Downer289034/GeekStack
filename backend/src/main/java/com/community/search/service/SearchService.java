package com.community.search.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.LikeEscape;
import com.community.common.PageResponse;
import com.community.forum.dto.PostCardResponse;
import com.community.forum.entity.Post;
import com.community.forum.mapper.PostMapper;
import com.community.share.dto.ShareCardResponse;
import com.community.share.entity.ProjectShare;
import com.community.share.mapper.ProjectShareMapper;
import org.springframework.stereotype.Service;

/**
 * G1 搜索服务。
 *
 * <p>LIKE 范围（一期定稿）：帖子 = {@code title OR content_md}；
 * 分享 = {@code title OR summary OR content_md}。
 * 不剥离 Markdown 符号，避免过度过滤漏检；正文噪声待产品反馈再收窄。
 *
 * <p>可见性：帖子依赖 MP 逻辑删除；分享强制 {@code status=PUBLISHED}（HIDDEN 不进结果，P2-6）。
 */
@Service
public class SearchService {

    private static final String TYPE_POST = "post";
    private static final String TYPE_SHARE = "share";

    private final PostMapper postMapper;
    private final ProjectShareMapper shareMapper;

    public SearchService(PostMapper postMapper, ProjectShareMapper shareMapper) {
        this.postMapper = postMapper;
        this.shareMapper = shareMapper;
    }

    /**
     * 按类型搜索。{@code type} 仅 {@code post|share}；空/缺 {@code q} → 400。
     */
    public PageResponse<?> search(String q, String type, long current, long size) {
        requireKeyword(q);
        String normalized = normalizeType(type);
        String escaped = LikeEscape.escape(q.trim());
        if (TYPE_POST.equals(normalized)) {
            return searchPosts(escaped, current, size);
        }
        return searchShares(escaped, current, size);
    }

    private PageResponse<PostCardResponse> searchPosts(String escaped, long current, long size) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        // ESCAPE '\\' + 参数化：已转义 %/_/\，禁止字符串拼接
        wrapper.apply("(title LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'"
                        + " OR content_md LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\')",
                escaped);
        wrapper.orderByDesc("created_at", "id");
        Page<Post> page = postMapper.selectPage(Page.of(current, size), wrapper);
        return PageResponse.from(page.convert(PostCardResponse::from));
    }

    private PageResponse<ShareCardResponse> searchShares(String escaped, long current, long size) {
        QueryWrapper<ProjectShare> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "PUBLISHED");
        wrapper.apply("(title LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'"
                        + " OR summary LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'"
                        + " OR content_md LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\')",
                escaped);
        wrapper.orderByDesc("created_at", "id");
        Page<ProjectShare> page = shareMapper.selectPage(Page.of(current, size), wrapper);
        return PageResponse.from(page.convert(this::toShareCard));
    }

    private ShareCardResponse toShareCard(ProjectShare share) {
        return new ShareCardResponse(share.getId(), share.getTitle(), share.getSummary(), share.getTags(),
                share.getRepoUrl(), share.getViewCount(), share.getCreatedAt());
    }

    private static void requireKeyword(String q) {
        if (q == null || q.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "搜索关键词不能为空");
        }
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "type 必填，仅支持 post|share");
        }
        String normalized = type.trim().toLowerCase();
        if (!TYPE_POST.equals(normalized) && !TYPE_SHARE.equals(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "type 仅支持 post|share");
        }
        return normalized;
    }
}

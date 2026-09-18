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
import com.community.skill.dto.SkillCardResponse;
import com.community.skill.entity.Skill;
import com.community.skill.mapper.SkillMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * G1 搜索服务。
 *
 * <p>LIKE 范围：帖子 = title|content_md；分享 = title|summary|content_md；
 * Skill = title|summary|content_md|model_adaptation_md。
 * 可见性：帖子依赖逻辑删除；分享/Skill 强制 PUBLISHED。
 */
@Service
public class SearchService {

    private static final String TYPE_POST = "post";
    private static final String TYPE_SHARE = "share";
    private static final String TYPE_SKILL = "skill";

    private final PostMapper postMapper;
    private final ProjectShareMapper shareMapper;
    private final SkillMapper skillMapper;

    public SearchService(PostMapper postMapper, ProjectShareMapper shareMapper, SkillMapper skillMapper) {
        this.postMapper = postMapper;
        this.shareMapper = shareMapper;
        this.skillMapper = skillMapper;
    }

    /**
     * 按类型搜索。{@code type} 仅 {@code post|share|skill}；空/缺 {@code q} → 400。
     */
    public PageResponse<?> search(String q, String type, long current, long size) {
        requireKeyword(q);
        String normalized = normalizeType(type);
        String escaped = LikeEscape.escape(q.trim());
        return switch (normalized) {
            case TYPE_POST -> searchPosts(escaped, current, size);
            case TYPE_SHARE -> searchShares(escaped, current, size);
            case TYPE_SKILL -> searchSkills(escaped, current, size);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "type 仅支持 post|share|skill");
        };
    }

    private PageResponse<PostCardResponse> searchPosts(String escaped, long current, long size) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
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

    private PageResponse<SkillCardResponse> searchSkills(String escaped, long current, long size) {
        QueryWrapper<Skill> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "PUBLISHED");
        wrapper.apply("(title LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'"
                        + " OR summary LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'"
                        + " OR content_md LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'"
                        + " OR IFNULL(model_adaptation_md, '') LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\')",
                escaped);
        wrapper.orderByDesc("created_at", "id");
        Page<Skill> page = skillMapper.selectPage(Page.of(current, size), wrapper);
        return PageResponse.from(page.convert(this::toSkillCard));
    }

    private ShareCardResponse toShareCard(ProjectShare share) {
        return new ShareCardResponse(share.getId(), share.getTitle(), share.getSummary(), share.getTags(),
                share.getRepoUrl(), share.getViewCount(), share.getCreatedAt());
    }

    private SkillCardResponse toSkillCard(Skill skill) {
        List<String> tags = skill.getTags() == null ? List.of() : List.copyOf(skill.getTags());
        return new SkillCardResponse(skill.getId(), skill.getUserId(), skill.getTitle(), skill.getSummary(),
                skill.getModelAdaptationMd(), skill.getVersion(), skill.getChangelog(), tags,
                skill.getViewCount(), skill.getLikeCount(), skill.getCreatedAt());
    }

    private static void requireKeyword(String q) {
        if (q == null || q.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "搜索关键词不能为空");
        }
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "type 必填，仅支持 post|share|skill");
        }
        String normalized = type.trim().toLowerCase();
        if (!TYPE_POST.equals(normalized) && !TYPE_SHARE.equals(normalized)
                && !TYPE_SKILL.equals(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "type 仅支持 post|share|skill");
        }
        return normalized;
    }
}

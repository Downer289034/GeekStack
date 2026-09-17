package com.community.skill.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.PageResponse;
import com.community.common.TagNormalizer;
import com.community.skill.dto.SkillCardResponse;
import com.community.skill.dto.SkillPayload;
import com.community.skill.dto.SkillResponse;
import com.community.skill.entity.Skill;
import com.community.skill.mapper.SkillMapper;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Skill：发布 / 编辑 / 下架 / 广场分页 / 详情。编辑与下架仅作者本人。
 */
@Service
public class SkillService {

    private static final int MAX_TAGS = 5;
    private static final String SORT_HOT = "hot";

    private final SkillMapper skillMapper;
    private final UserService userService;

    public SkillService(SkillMapper skillMapper, UserService userService) {
        this.skillMapper = skillMapper;
        this.userService = userService;
    }

    /** 发布 Skill。 */
    public SkillResponse create(String username, SkillPayload payload) {
        long userId = requireUserId(username);
        Skill skill = new Skill();
        skill.setUserId(userId);
        applyPayload(skill, payload);
        skill.setStatus("PUBLISHED");
        skill.setViewCount(0L);
        skill.setLikeCount(0L);
        skillMapper.insert(skill);
        return toResponse(skill);
    }

    /** 编辑，仅作者。 */
    public SkillResponse update(long id, String username, SkillPayload payload) {
        Skill skill = requireOwned(id, username);
        applyPayload(skill, payload);
        skillMapper.updateById(skill);
        return toResponse(skill);
    }

    /** 下架（HIDDEN），仅作者。 */
    public SkillResponse hide(long id, String username) {
        Skill skill = requireOwned(id, username);
        skill.setStatus("HIDDEN");
        skillMapper.updateById(skill);
        return toResponse(skill);
    }

    /**
     * 广场分页：仅 PUBLISHED；可选 tag；sort=latest|hot（hot≈ view + like*2）。
     */
    public PageResponse<SkillCardResponse> pagePublished(long current, long size, String tag, String sort) {
        QueryWrapper<Skill> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "PUBLISHED");
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag.trim());
        }
        if (SORT_HOT.equalsIgnoreCase(sort)) {
            wrapper.orderByDesc("view_count + like_count * 2", "id");
        } else {
            wrapper.orderByDesc("created_at", "id");
        }
        Page<Skill> page = skillMapper.selectPage(Page.of(current, size), wrapper);
        return PageResponse.from(page.convert(this::toCard));
    }

    /**
     * 详情：浏览数 +1；HIDDEN 仅作者可见。
     *
     * @param username 未登录传 null
     */
    public SkillResponse getDetail(long id, String username) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Skill 不存在");
        }
        boolean isOwner = username != null && skill.getUserId().equals(requireUserId(username));
        if ("HIDDEN".equals(skill.getStatus()) && !isOwner) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Skill 不存在");
        }
        skillMapper.update(null, Wrappers.<Skill>lambdaUpdate()
                .eq(Skill::getId, id)
                .setSql("view_count = view_count + 1"));
        skill.setViewCount(skill.getViewCount() == null ? 1 : skill.getViewCount() + 1);
        return toResponse(skill);
    }

    private void applyPayload(Skill skill, SkillPayload payload) {
        skill.setTitle(payload.title());
        skill.setSummary(payload.summary());
        skill.setContentMd(payload.contentMd());
        skill.setModelAdaptationMd(payload.modelAdaptationMd());
        skill.setTags(TagNormalizer.normalize(payload.tags(), MAX_TAGS));
    }

    private SkillCardResponse toCard(Skill skill) {
        List<String> tags = skill.getTags() == null ? List.of() : List.copyOf(skill.getTags());
        return new SkillCardResponse(skill.getId(), skill.getUserId(), skill.getTitle(), skill.getSummary(),
                skill.getModelAdaptationMd(), tags, skill.getViewCount(), skill.getLikeCount(),
                skill.getCreatedAt());
    }

    private SkillResponse toResponse(Skill skill) {
        List<String> tags = skill.getTags() == null ? List.of() : List.copyOf(skill.getTags());
        return new SkillResponse(skill.getId(), skill.getUserId(), skill.getTitle(), skill.getSummary(),
                skill.getContentMd(), skill.getModelAdaptationMd(), tags, skill.getStatus(),
                skill.getViewCount(), skill.getLikeCount(), skill.getCreatedAt());
    }

    private Skill requireOwned(long id, String username) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Skill 不存在");
        }
        if (!skill.getUserId().equals(requireUserId(username))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅限作者本人操作");
        }
        return skill;
    }

    private long requireUserId(String username) {
        UserResponse user = userService.me(username);
        return user.id();
    }
}

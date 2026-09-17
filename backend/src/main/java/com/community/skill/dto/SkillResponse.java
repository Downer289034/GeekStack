package com.community.skill.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 详情响应。
 */
public record SkillResponse(
        Long id,
        Long userId,
        String title,
        String summary,
        String contentMd,
        String modelAdaptationMd,
        List<String> tags,
        String status,
        Long viewCount,
        Long likeCount,
        LocalDateTime createdAt
) {
}

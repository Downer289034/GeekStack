package com.community.skill.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 广场卡片（不含提示词正文）。
 */
public record SkillCardResponse(
        Long id,
        Long userId,
        String title,
        String summary,
        String modelAdaptationMd,
        List<String> tags,
        Long viewCount,
        Long likeCount,
        LocalDateTime createdAt
) {
}

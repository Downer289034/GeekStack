package com.community.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建/编辑 Skill 请求。version 空则默认 1.0.0；非空须为 SemVer 核心格式。
 */
public record SkillPayload(
        @NotBlank @Size(max = 200)
        String title,

        @NotBlank @Size(max = 500)
        String summary,

        @NotBlank
        String contentMd,

        @Size(max = 1000)
        String modelAdaptationMd,

        /** SemVer；null/空白 → 服务端默认 1.0.0。 */
        @Size(max = 32)
        String version,

        @Size(max = 2000)
        String changelog,

        @Size(max = 5, message = "标签最多 5 个")
        List<String> tags
) {
}

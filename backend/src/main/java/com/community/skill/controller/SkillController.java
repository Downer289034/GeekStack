package com.community.skill.controller;

import com.community.common.PageResponse;
import com.community.common.Result;
import com.community.skill.dto.SkillCardResponse;
import com.community.skill.dto.SkillPayload;
import com.community.skill.dto.SkillResponse;
import com.community.skill.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Skill 广场：GET 公开；发布/编辑/下架需登录。
 */
@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /** 广场分页：tag 可选，sort=latest|hot。 */
    @GetMapping
    public Result<PageResponse<SkillCardResponse>> page(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String tag,
                                                        @RequestParam(defaultValue = "latest") String sort) {
        return Result.ok(skillService.pagePublished(current, size, tag, sort));
    }

    /** 详情（HIDDEN 非作者 404）。 */
    @GetMapping("/{id}")
    public Result<SkillResponse> detail(@PathVariable long id, Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        return Result.ok(skillService.getDetail(id, username));
    }

    /** 发布。 */
    @PostMapping
    public Result<SkillResponse> create(@Valid @RequestBody SkillPayload payload,
                                        Authentication authentication) {
        return Result.ok(skillService.create(authentication.getName(), payload));
    }

    /** 编辑（仅作者）。 */
    @PutMapping("/{id}")
    public Result<SkillResponse> update(@PathVariable long id,
                                        @Valid @RequestBody SkillPayload payload,
                                        Authentication authentication) {
        return Result.ok(skillService.update(id, authentication.getName(), payload));
    }

    /** 下架（HIDDEN，仅作者）。 */
    @PostMapping("/{id}/hide")
    public Result<SkillResponse> hide(@PathVariable long id, Authentication authentication) {
        return Result.ok(skillService.hide(id, authentication.getName()));
    }
}

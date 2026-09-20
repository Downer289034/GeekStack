package com.community.search.controller;

import com.community.common.Result;
import com.community.search.dto.TagCountResponse;
import com.community.search.service.TagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * G2 / T-BE-4.2 标签云：公开；聚合可见帖子 + PUBLISHED 分享/Skill。
 */
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /** 标签云 {@code [{name, count}]}；无数据返回空数组。 */
    @GetMapping
    public Result<List<TagCountResponse>> list() {
        return Result.ok(tagService.listTags());
    }
}

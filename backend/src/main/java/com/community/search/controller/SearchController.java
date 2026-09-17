package com.community.search.controller;

import com.community.common.PageResponse;
import com.community.common.Result;
import com.community.search.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * G1 搜索接口：公开；{@code type} 仅 {@code post|share|skill}（不做 type=all 混合分页）。
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** 关键字搜索（空 q / 非法 type → 400）。 */
    @GetMapping
    public Result<PageResponse<?>> search(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(searchService.search(q, type, current, size));
    }
}

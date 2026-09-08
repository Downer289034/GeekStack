package com.community.hot.controller;

import com.community.common.Result;
import com.community.forum.dto.PostCardResponse;
import com.community.hot.service.HotPostService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 热帖榜单接口（T21）：公开，60s 缓存，Redis 不可用时降级到 DB。
 * 全站热帖，不限定用户。
 */
@RestController
@RequestMapping("/api/v1/hot")
public class HotPostController {

    private final HotPostService hotPostService;

    public HotPostController(HotPostService hotPostService) {
        this.hotPostService = hotPostService;
    }

    /** 热帖榜（默认 10 条）。 */
    @GetMapping
    public Result<List<PostCardResponse>> hot(@RequestParam(defaultValue = "10") long size) {
        return Result.ok(hotPostService.getHotList(size));
    }
}

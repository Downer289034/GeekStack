package com.community.hot.controller;

import com.community.common.Result;
import com.community.forum.dto.PostCardResponse;
import com.community.hot.service.HotPostService;
import com.community.user.service.UserService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 热帖榜单接口（T21）：公开，60s 缓存，Redis 不可用时降级到 DB。
 */
@RestController
@RequestMapping("/api/v1/hot")
public class HotPostController {

    private final HotPostService hotPostService;
    private final UserService userService;

    public HotPostController(HotPostService hotPostService, UserService userService) {
        this.hotPostService = hotPostService;
        this.userService = userService;
    }

    /** 热帖榜（默认 10 条）。 */
    @GetMapping
    public Result<List<PostCardResponse>> hot(@RequestParam(defaultValue = "10") long size,
                                               Authentication authentication) {
        long userId = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                ? userService.me(authentication.getName()).id() : 0;
        return Result.ok(hotPostService.getHotList(size, userId));
    }
}

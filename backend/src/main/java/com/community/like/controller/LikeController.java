package com.community.like.controller;

import com.community.common.Result;
import com.community.like.service.LikeService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 点赞接口（T19）：
 * - POST /likes/{targetType}/{targetId} toggle（认证必需）
 * - GET /likes/status?targetType=POST&targetIds=1,2,3 批量查询点赞状态
 */
@RestController
@RequestMapping("/api/v1/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /** toggle 点赞：已赞→取消，未赞→点赞，返回当前状态。 */
    @PostMapping("/{targetType}/{targetId}")
    public Result<Boolean> toggle(@PathVariable String targetType,
                                   @PathVariable long targetId,
                                   Authentication authentication) {
        return Result.ok(likeService.toggle(authentication.getName(), targetType, targetId));
    }

    /** 批量查询用户对帖子列表的点赞状态。 */
    @GetMapping("/status")
    public Result<List<Long>> likedStatus(@RequestParam String targetType,
                                          @RequestParam String targetIds,
                                          Authentication authentication) {
        long userId = likeService.requireUserId(authentication.getName());
        List<Long> ids = java.util.Arrays.stream(targetIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
        return Result.ok(likeService.getLikedPostIds(userId, ids).stream().toList());
    }
}

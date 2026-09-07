package com.community.user.controller;

import com.community.common.Result;
import com.community.user.dto.ProfileAggregateResponse;
import com.community.user.dto.UserResponse;
import com.community.user.service.ProfileService;
import com.community.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：me 需认证；{username}/profile 公开（未登录 viewer 传 null，HIDDEN 不展示）。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    public UserController(UserService userService, ProfileService profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    /** 当前登录用户资料；principal 为 JWT subject（用户名）。 */
    @GetMapping("/me")
    public Result<UserResponse> me(Authentication authentication) {
        return Result.ok(userService.me(authentication.getName()));
    }

    /**
     * 个人主页聚合（公开）：匿名访问时 HIDDEN 分享不可见；本人登录访问时包含 HIDDEN。
     * permitAll 路径下匿名请求按未登录处理（与 share 详情同口径）。
     */
    @GetMapping("/{username}/profile")
    public Result<ProfileAggregateResponse> profile(@PathVariable String username,
                                                    Authentication authentication) {
        String viewer = authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())
                ? null
                : authentication.getName();
        return Result.ok(profileService.profile(username, viewer));
    }
}

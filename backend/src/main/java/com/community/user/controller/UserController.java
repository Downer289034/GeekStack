package com.community.user.controller;

import com.community.common.Result;
import com.community.user.dto.MeResponse;
import com.community.user.dto.ProfileAggregateResponse;
import com.community.user.dto.UpdateMeRequest;
import com.community.user.dto.UpdatePasswordRequest;
import com.community.user.service.ProfileService;
import com.community.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 当前登录用户资料（含 hasPassword / oauthBindings）；principal 为 JWT subject（用户名）。 */
    @GetMapping("/me")
    public Result<MeResponse> me(Authentication authentication) {
        return Result.ok(userService.meDetail(authentication.getName()));
    }

    /** 更新当前用户昵称/简介/头像 URL。 */
    @PutMapping("/me")
    public Result<MeResponse> updateMe(Authentication authentication,
                                       @Valid @RequestBody UpdateMeRequest request) {
        return Result.ok(userService.updateMe(authentication.getName(), request));
    }

    /**
     * 设置或修改密码。无密用户首设免验旧密码；已有密码须验旧。
     * 成功后旧 refresh 立即失效（见 UserService.changePassword 口径）。
     */
    @PutMapping("/me/password")
    public Result<Void> updatePassword(Authentication authentication,
                                       @Valid @RequestBody UpdatePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return Result.ok();
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

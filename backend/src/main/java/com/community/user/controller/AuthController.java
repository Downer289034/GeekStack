package com.community.user.controller;

import com.community.common.Result;
import com.community.user.dto.LoginRequest;
import com.community.user.dto.RefreshRequest;
import com.community.user.dto.RegisterRequest;
import com.community.user.dto.TokenResponse;
import com.community.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 刷新令牌（公开访问）。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 注册并直接登录，返回令牌对。 */
    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    /** 用户名密码登录，返回令牌对。 */
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    /** 刷新令牌，返回新令牌对。 */
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(userService.refresh(request.refreshToken()));
    }
}

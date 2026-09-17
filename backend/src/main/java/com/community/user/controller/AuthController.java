package com.community.user.controller;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.Result;
import com.community.user.dto.LoginRequest;
import com.community.user.dto.RefreshRequest;
import com.community.user.dto.RegisterRequest;
import com.community.user.dto.TokenResponse;
import com.community.user.service.AuthRateLimiter;
import com.community.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 刷新令牌（公开访问；失败频控）。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthRateLimiter authRateLimiter;

    public AuthController(UserService userService, AuthRateLimiter authRateLimiter) {
        this.userService = userService;
        this.authRateLimiter = authRateLimiter;
    }

    /** 注册并直接登录，返回令牌对。 */
    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request,
                                          HttpServletRequest http) {
        String ip = clientIp(http);
        authRateLimiter.checkAllowed(ip, request.username());
        try {
            TokenResponse tokens = userService.register(request);
            authRateLimiter.clear(ip, request.username());
            return Result.ok(tokens);
        } catch (BusinessException e) {
            recordIfAuthFailure(ip, request.username(), e);
            throw e;
        }
    }

    /** 用户名密码登录，返回令牌对。 */
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest http) {
        String ip = clientIp(http);
        authRateLimiter.checkAllowed(ip, request.username());
        try {
            TokenResponse tokens = userService.login(request);
            authRateLimiter.clear(ip, request.username());
            return Result.ok(tokens);
        } catch (BusinessException e) {
            recordIfAuthFailure(ip, request.username(), e);
            throw e;
        }
    }

    /** 刷新令牌，返回新令牌对。 */
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                         HttpServletRequest http) {
        String ip = clientIp(http);
        String subject = "refresh";
        authRateLimiter.checkAllowed(ip, subject);
        try {
            TokenResponse tokens = userService.refresh(request.refreshToken());
            authRateLimiter.clear(ip, subject);
            return Result.ok(tokens);
        } catch (BusinessException e) {
            recordIfAuthFailure(ip, subject, e);
            throw e;
        }
    }

    private void recordIfAuthFailure(String ip, String username, BusinessException e) {
        ErrorCode code = e.getErrorCode();
        if (code == ErrorCode.UNAUTHORIZED || code == ErrorCode.FORBIDDEN || code == ErrorCode.CONFLICT) {
            authRateLimiter.recordFailure(ip, username);
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

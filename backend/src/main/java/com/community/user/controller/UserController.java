package com.community.user.controller;

import com.community.common.Result;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口（需认证）。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 当前登录用户资料；principal 为 JWT subject（用户名）。 */
    @GetMapping("/me")
    public Result<UserResponse> me(Authentication authentication) {
        return Result.ok(userService.me(authentication.getName()));
    }
}

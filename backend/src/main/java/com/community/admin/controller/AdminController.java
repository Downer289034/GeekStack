package com.community.admin.controller;

import com.community.admin.dto.UpdateUserStatusRequest;
import com.community.admin.service.AdminService;
import com.community.common.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端最小集（T-M4-1）：删帖 / 禁言解封。路径保护见 SecurityConfig hasRole('ADMIN')。
 * 运维执行 promote-admin.sql 后，目标用户须重新登录（或 refresh）换发带 role 的 JWT；
 * 旧令牌无 ADMIN → 403 为预期，勿当 bug。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 管理员删帖（逻辑删除，口径与作者自删一致）。 */
    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable long id, Authentication authentication) {
        adminService.deletePost(id, authentication.getName());
        return Result.ok();
    }

    /** 禁言/解封：status=0 正常，1 封禁。 */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable long id,
                                         @Valid @RequestBody UpdateUserStatusRequest request,
                                         Authentication authentication) {
        adminService.updateUserStatus(id, request, authentication.getName());
        return Result.ok();
    }
}

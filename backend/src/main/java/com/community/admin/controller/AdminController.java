package com.community.admin.controller;

import com.community.admin.dto.UpdatePostFeaturedRequest;
import com.community.admin.dto.UpdatePostPinnedRequest;
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
 * 管理端（T-M4-1 + T-ADMIN-FEATURE）：删帖 / 禁言 / 加精 / 置顶。
 * 路径保护见 SecurityConfig hasRole('ADMIN')（含 steward_bot）。
 * 运维提权后目标用户须重新登录换发带 role 的 JWT；旧令牌无 ADMIN → 403 为预期。
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

    /** 加精/取消：featured=1 加精，0 取消（幂等）。 */
    @PutMapping("/posts/{id}/featured")
    public Result<Void> updatePostFeatured(@PathVariable long id,
                                           @Valid @RequestBody UpdatePostFeaturedRequest request,
                                           Authentication authentication) {
        adminService.updatePostFeatured(id, request, authentication.getName());
        return Result.ok();
    }

    /** 置顶/取消：pinned=1 置顶，0 取消（幂等）。 */
    @PutMapping("/posts/{id}/pinned")
    public Result<Void> updatePostPinned(@PathVariable long id,
                                         @Valid @RequestBody UpdatePostPinnedRequest request,
                                         Authentication authentication) {
        adminService.updatePostPinned(id, request, authentication.getName());
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

package com.community.oauth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.Result;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserRepoMapper;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓库隐藏/取消隐藏接口（T17 验收硬条件）。
 */
@RestController
@RequestMapping("/api/v1/repos")
public class RepoHideController {

    private final UserRepoMapper repoMapper;
    private final UserService userService;

    public RepoHideController(UserRepoMapper repoMapper, UserService userService) {
        this.repoMapper = repoMapper;
        this.userService = userService;
    }

    /** 切换仓库隐藏状态：0 ↔ 1。仅限仓库所属用户操作。 */
    @PostMapping("/{id}/toggle-hide")
    public Result<Boolean> toggleHide(@PathVariable long id, Authentication authentication) {
        UserRepo repo = repoMapper.selectById(id);
        if (repo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在");
        }
        UserResponse me = userService.me(authentication.getName());
        if (!repo.getUserId().equals(me.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅限仓库所属用户操作");
        }
        int newHidden = repo.getHidden() == null || repo.getHidden() == 0 ? 1 : 0;
        repoMapper.update(null, Wrappers.<UserRepo>lambdaUpdate()
                .eq(UserRepo::getId, id)
                .set(UserRepo::getHidden, newHidden));
        return Result.ok(newHidden == 1);
    }
}

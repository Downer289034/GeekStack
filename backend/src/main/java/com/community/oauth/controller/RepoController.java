package com.community.oauth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.Result;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserRepoMapper;
import com.community.oauth.service.RepoSyncService;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓库接口（T17）：
 * - GET /users/{username}/repos：公开（隐藏对所有人不展示，与 share 的 HIDDEN 不同）
 * - POST /repos/{id}/toggle-hide：隐藏/取消（仅所属用户，认证）
 */
@RestController
@RequestMapping("/api/v1")
public class RepoController {

    private final UserRepoMapper repoMapper;
    private final UserService userService;
    private final RepoSyncService repoSyncService;

    public RepoController(UserRepoMapper repoMapper, UserService userService,
                          RepoSyncService repoSyncService) {
        this.repoMapper = repoMapper;
        this.userService = userService;
        this.repoSyncService = repoSyncService;
    }

    /** 用户仓库列表（公开，默认 hidden=0；导入时传 includeHidden=true）。 */
    @GetMapping("/users/{username}/repos")
    public Result<List<UserRepo>> listRepos(@PathVariable String username,
                                             @RequestParam(defaultValue = "false") boolean includeHidden) {
        UserResponse user = userService.me(username);
        var wrapper = Wrappers.<UserRepo>lambdaQuery().eq(UserRepo::getUserId, user.id());
        if (!includeHidden) {
            wrapper.eq(UserRepo::getHidden, 0);
        }
        return Result.ok(repoMapper.selectList(wrapper.orderByDesc(UserRepo::getStars)));
    }

    /** 切换仓库隐藏状态（仅所属用户）。 */
    @PostMapping("/repos/{id}/toggle-hide")
    public Result<Boolean> toggleHide(@PathVariable long id, Authentication authentication) {
        UserResponse me = userService.me(authentication.getName());
        return Result.ok(repoSyncService.toggleHide(id, me.id()));
    }
}

package com.community.oauth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.Result;
import com.community.oauth.dto.ManualRepoSyncRequest;
import com.community.oauth.dto.ManualRepoSyncResponse;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserRepoMapper;
import com.community.oauth.service.RepoSyncRateLimiter;
import com.community.oauth.service.RepoSyncService;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓库接口（T17 + T-REPO-MANUAL-SYNC-API）：
 * - GET /users/{username}/repos：公开（隐藏对所有人不展示，与 share 的 HIDDEN 不同）
 * - POST /repos/{id}/toggle-hide：隐藏/取消（仅所属用户，认证）
 * - POST /me/repos/sync：本人手动同步指定 provider 快照（认证 + 冷却防抖）
 */
@RestController
@RequestMapping("/api/v1")
public class RepoController {

    private final UserRepoMapper repoMapper;
    private final UserService userService;
    private final RepoSyncService repoSyncService;
    private final RepoSyncRateLimiter repoSyncRateLimiter;

    public RepoController(UserRepoMapper repoMapper, UserService userService,
                          RepoSyncService repoSyncService,
                          RepoSyncRateLimiter repoSyncRateLimiter) {
        this.repoMapper = repoMapper;
        this.userService = userService;
        this.repoSyncService = repoSyncService;
        this.repoSyncRateLimiter = repoSyncRateLimiter;
    }

    /**
     * 用户仓库列表（公开）：
     * - 默认 hidden=0（仅展示可见仓库），任何人可查
     * - includeHidden=true 时返回全量（含用户隐藏的）——仅限本人，否则 403
     */
    @GetMapping("/users/{username}/repos")
    public Result<List<UserRepo>> listRepos(@PathVariable String username,
                                             @RequestParam(defaultValue = "false") boolean includeHidden,
                                             Authentication authentication) {
        UserResponse user = userService.me(username);
        if (includeHidden) {
            boolean isOwner = authentication != null && authentication.isAuthenticated()
                    && username.equals(authentication.getName());
            if (!isOwner) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅限本人查看隐藏仓库");
            }
        }
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

    /**
     * 手动同步当前用户仓库快照。
     * {@code provider} 取自 query（优先）或 JSON body；取值 GITHUB / GITEE（大小写不敏感）。
     * 冷却默认 60s（进程内内存），见 {@code community.oauth.repo-sync-cooldown-seconds}。
     */
    @PostMapping("/me/repos/sync")
    public Result<ManualRepoSyncResponse> syncMyRepos(
            @RequestParam(required = false) String provider,
            @RequestBody(required = false) ManualRepoSyncRequest body,
            Authentication authentication) {
        String raw = (provider != null && !provider.isBlank())
                ? provider
                : (body == null ? null : body.provider());
        String normalized = RepoSyncService.normalizeProvider(raw);
        UserResponse me = userService.me(authentication.getName());
        repoSyncRateLimiter.checkAndMark(me.id(), normalized);
        int count = repoSyncService.syncReposBlocking(me.id(), normalized);
        return Result.ok(new ManualRepoSyncResponse(normalized, count));
    }
}

package com.community.share.controller;

import com.community.common.PageResponse;
import com.community.common.Result;
import com.community.share.dto.ShareCardResponse;
import com.community.share.dto.SharePayload;
import com.community.share.dto.ShareResponse;
import com.community.share.service.ShareService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目分享接口：广场列表与详情公开（GET），发布/编辑/下架需认证。
 */
@RestController
@RequestMapping("/api/v1/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /** 广场分页列表（公开）：仅 PUBLISHED，支持单标签筛选。 */
    @GetMapping
    public Result<PageResponse<ShareCardResponse>> page(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String tag) {
        return Result.ok(shareService.pagePublished(current, size, tag));
    }

    /**
     * 项目详情（公开）：浏览数 +1；HIDDEN 仅作者可见。
     * permitAll 路径下匿名请求的 authentication 为 AnonymousAuthenticationToken，按未登录处理。
     */
    @GetMapping("/{id}")
    public Result<ShareResponse> detail(@PathVariable long id, Authentication authentication) {
        String username = authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())
                ? null
                : authentication.getName();
        return Result.ok(shareService.getDetail(id, username));
    }

    /** 发布项目分享。 */
    @PostMapping
    public Result<ShareResponse> create(@Valid @RequestBody SharePayload payload,
                                        Authentication authentication) {
        return Result.ok(shareService.create(authentication.getName(), payload));
    }

    /** 编辑项目分享（仅限作者本人）。 */
    @PutMapping("/{id}")
    public Result<ShareResponse> update(@PathVariable long id,
                                        @Valid @RequestBody SharePayload payload,
                                        Authentication authentication) {
        return Result.ok(shareService.update(id, authentication.getName(), payload));
    }

    /** 下架项目分享（仅限作者本人）。 */
    @PostMapping("/{id}/hide")
    public Result<ShareResponse> hide(@PathVariable long id, Authentication authentication) {
        return Result.ok(shareService.hide(id, authentication.getName()));
    }
}

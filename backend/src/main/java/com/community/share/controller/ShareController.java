package com.community.share.controller;

import com.community.common.Result;
import com.community.share.dto.SharePayload;
import com.community.share.dto.ShareResponse;
import com.community.share.service.ShareService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目分享接口（均需认证）：发布 / 编辑 / 下架。广场列表与详情见 Task 10。
 */
@RestController
@RequestMapping("/api/v1/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
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

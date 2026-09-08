package com.community.notify.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.PageResponse;
import com.community.common.Result;
import com.community.notify.entity.Notification;
import com.community.notify.mapper.NotificationMapper;
import com.community.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知接口（T20）：分页查询 + 未读数 + 已读标记（单条/全部）。
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationMapper notificationMapper;
    private final UserService userService;

    public NotificationController(NotificationMapper notificationMapper, UserService userService) {
        this.notificationMapper = notificationMapper;
        this.userService = userService;
    }

    /** 通知分页列表（按创建时间倒序，仅读本人通知）。 */
    @GetMapping
    public Result<PageResponse<Notification>> list(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "20") long size,
                                                    Authentication authentication) {
        long userId = userService.me(authentication.getName()).id();
        Page<Notification> page = notificationMapper.selectPage(
                Page.of(current, size),
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt));
        return Result.ok(PageResponse.from(page));
    }

    /** 未读通知数。 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(Authentication authentication) {
        long userId = userService.me(authentication.getName()).id();
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return Result.ok(count);
    }

    /** 标记单条通知已读。 */
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable long id, Authentication authentication) {
        long userId = userService.me(authentication.getName()).id();
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
        return Result.ok();
    }

    /** 标记全部通知已读。 */
    @PostMapping("/read-all")
    public Result<Void> markAllRead(Authentication authentication) {
        long userId = userService.me(authentication.getName()).id();
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
        return Result.ok();
    }
}

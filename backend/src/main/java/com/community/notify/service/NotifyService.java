package com.community.notify.service;

import com.community.event.ContentCreatedEvent;
import com.community.event.ContentLikedEvent;
import com.community.event.ContentRepliedEvent;
import com.community.notify.entity.Notification;
import com.community.notify.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 站内通知服务（T20）：
 * - 只消费事件，不依赖其他业务模块的代码（模块间零代码依赖）
 * - 自己赞自己/自己回自己 → 不产生通知（防自嗨）
 * - 写入 notification 表（V1 已建好）
 */
@Service
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);
    private static final int PREVIEW_MAX = 200;

    private final NotificationMapper notificationMapper;

    public NotifyService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Async
    @EventListener
    public void onContentLiked(ContentLikedEvent event) {
        // 防自嗨：不通知自己
        if (event.getLikerUserId() == event.getTargetAuthorUserId() || event.getTargetAuthorUserId() <= 0) {
            return;
        }
        String targetName = targetTypeName(event.getTargetType());
        Notification notification = new Notification();
        notification.setUserId(event.getTargetAuthorUserId());
        notification.setType("LIKE");
        notification.setTitle("你的" + targetName + "收到新点赞");
        notification.setContent(targetName + "被用户 #" + event.getLikerUserId() + " 点赞");
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        log.info("站内通知(LIKE): userId={}, targetId={}", event.getTargetAuthorUserId(), event.getTargetId());
    }

    @Async
    @EventListener
    public void onContentReplied(ContentRepliedEvent event) {
        if (event.getReplierUserId() == event.getTargetAuthorUserId()) {
            return; // 防自嗨
        }
        String targetName = targetTypeName(event.getTargetType());
        String preview = event.getReplyPreview();
        if (preview != null && preview.length() > PREVIEW_MAX) {
            preview = preview.substring(0, PREVIEW_MAX) + "…";
        }
        Notification notification = new Notification();
        notification.setUserId(event.getTargetAuthorUserId());
        notification.setType("REPLY");
        notification.setTitle("你的" + targetName + "有新回帖");
        notification.setContent(preview);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        log.info("站内通知(REPLY): userId={}, targetId={}", event.getTargetAuthorUserId(), event.getTargetId());
    }

    private String targetTypeName(String targetType) {
        // P2-15 修复：事件传值有大小写两种（ContentCreatedEvent 传小写，ContentRepliedEvent 传 ENUM 大写）
        return switch (targetType) {
            case "POST", "post" -> "帖子";
            case "SHARE", "share" -> "项目分享";
            case "SKILL", "skill" -> "Skill";
            case "COMMENT", "comment" -> "评论";
            default -> "内容";
        };
    }
}

package com.community.event;

import org.springframework.context.ApplicationEvent;

/**
 * 点赞成功事件（T19 预埋）：T19 只发布不消费，T20 notify 模块来接。
 */
public class ContentLikedEvent extends ApplicationEvent {

    private final String targetType;
    private final long targetId;
    private final long likerUserId;
    private final long targetAuthorUserId;

    public ContentLikedEvent(Object source, String targetType, long targetId,
                             long likerUserId, long targetAuthorUserId) {
        super(source);
        this.targetType = targetType;
        this.targetId = targetId;
        this.likerUserId = likerUserId;
        this.targetAuthorUserId = targetAuthorUserId;
    }

    public String getTargetType() {
        return targetType;
    }

    public long getTargetId() {
        return targetId;
    }

    public long getLikerUserId() {
        return likerUserId;
    }

    public long getTargetAuthorUserId() {
        return targetAuthorUserId;
    }
}

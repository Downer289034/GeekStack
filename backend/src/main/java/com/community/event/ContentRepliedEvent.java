package com.community.event;

import org.springframework.context.ApplicationEvent;

/**
 * 回帖成功事件（T20 新增）：评论创建时发布，notify 模块消费写站内通知。
 * 与 ContentCreatedEvent（webhook 推送给 Agent）不同——事件不混流：
 * - ContentCreatedEvent：webhook 侧，只推 created 动作
 * - ContentRepliedEvent：通知侧，推 replied 动作（含宿主信息供通知展示）
 */
public class ContentRepliedEvent extends ApplicationEvent {

    private final String targetType;
    private final long targetId;
    private final long replierUserId;
    private final long targetAuthorUserId;
    private final String replyPreview;

    public ContentRepliedEvent(Object source, String targetType, long targetId,
                               long replierUserId, long targetAuthorUserId, String replyPreview) {
        super(source);
        this.targetType = targetType;
        this.targetId = targetId;
        this.replierUserId = replierUserId;
        this.targetAuthorUserId = targetAuthorUserId;
        this.replyPreview = replyPreview;
    }

    public String getTargetType() { return targetType; }
    public long getTargetId() { return targetId; }
    public long getReplierUserId() { return replierUserId; }
    public long getTargetAuthorUserId() { return targetAuthorUserId; }
    public String getReplyPreview() { return replyPreview; }
}

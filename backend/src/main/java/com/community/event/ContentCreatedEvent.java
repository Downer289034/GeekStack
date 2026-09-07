package com.community.event;

import org.springframework.context.ApplicationEvent;

/**
 * 帖子/评论/分享创建事件：业务层发布，WebhookListener 消费后推送给 Agent。
 * 只有 created 动作（P0/P1 约定），编辑/删除不推。
 */
public abstract class ContentCreatedEvent extends ApplicationEvent {

    private final long contentId;
    private final String contentType;
    private final String contentText;
    private final long authorId;

    protected ContentCreatedEvent(Object source, long contentId, String contentType,
                                   String contentText, long authorId) {
        super(source);
        this.contentId = contentId;
        this.contentType = contentType;
        this.contentText = contentText;
        this.authorId = authorId;
    }

    public long getContentId() {
        return contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentText() {
        return contentText;
    }

    public long getAuthorId() {
        return authorId;
    }

    /** 帖子创建事件。 */
    public static class PostCreated extends ContentCreatedEvent {
        public PostCreated(Object source, long postId, String contentText, long authorId) {
            super(source, postId, "post", contentText, authorId);
        }
    }

    /** 评论创建事件（targetType = "post" 或 "share"）。 */
    public static class CommentCreated extends ContentCreatedEvent {
        private final String targetType;

        public CommentCreated(Object source, long commentId, String contentText,
                              long authorId, String targetType) {
            super(source, commentId, "comment", contentText, authorId);
            this.targetType = targetType;
        }

        public String getTargetType() {
            return targetType;
        }
    }

    /** 分享创建事件。 */
    public static class ShareCreated extends ContentCreatedEvent {
        public ShareCreated(Object source, long shareId, String contentText, long authorId) {
            super(source, shareId, "share", contentText, authorId);
        }
    }
}

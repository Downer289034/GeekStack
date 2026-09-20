package com.community.rag.event;

import org.springframework.context.ApplicationEvent;

/**
 * RAG 索引变更事件：UPSERT=公开内容入/换库，DELETE=删帖/下架同步删向量。
 */
public class RagContentChangedEvent extends ApplicationEvent {

    public enum Action {
        UPSERT,
        DELETE
    }

    private final Action action;
    private final String contentType;
    private final long contentId;

    public RagContentChangedEvent(Object source, Action action, String contentType, long contentId) {
        super(source);
        this.action = action;
        this.contentType = contentType;
        this.contentId = contentId;
    }

    public static RagContentChangedEvent upsert(Object source, String contentType, long contentId) {
        return new RagContentChangedEvent(source, Action.UPSERT, contentType, contentId);
    }

    public static RagContentChangedEvent delete(Object source, String contentType, long contentId) {
        return new RagContentChangedEvent(source, Action.DELETE, contentType, contentId);
    }

    public Action getAction() {
        return action;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentId() {
        return contentId;
    }
}

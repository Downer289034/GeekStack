package com.community.rag;

import com.community.rag.event.RagContentChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * RAG 索引消费者：异步处理 UPSERT/DELETE，失败仅 WARN。
 */
@Component
public class RagIndexListener {

    private static final Logger log = LoggerFactory.getLogger(RagIndexListener.class);

    private final RagIndexService ragIndexService;
    private final RagProperties properties;

    public RagIndexListener(RagIndexService ragIndexService, RagProperties properties) {
        this.ragIndexService = ragIndexService;
        this.properties = properties;
    }

    @Async
    @EventListener
    public void onContentChanged(RagContentChangedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            if (event.getAction() == RagContentChangedEvent.Action.DELETE) {
                ragIndexService.delete(event.getContentType(), event.getContentId());
            } else {
                ragIndexService.upsert(event.getContentType(), event.getContentId());
            }
        } catch (Exception e) {
            log.warn("RAG listener 异常(不阻塞): action={} type={} id={} error={}",
                    event.getAction(), event.getContentType(), event.getContentId(), e.getMessage());
        }
    }
}

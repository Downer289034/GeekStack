package com.community.event;

import com.community.event.dto.EventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 异步 Webhook 推送监听器：接收 ContentCreatedEvent → 构造 EventPayload → HMAC 签名 → POST Agent。
 *
 * 行为口径（契约 §6）：
 * - 202 → 记 trace_id（可对账）
 * - 401 / 5xx / 超时 → 记 WARN 日志，不重试，不阻塞主流程
 * - 422 → 记 ERROR 日志（契约 bug 信号），上报 Agent PM
 * - content_text 上限 5000 字符，超长截断并置 truncated=true
 *
 * 安全红线：签名密钥走环境变量，不入日志/入 git。
 */
@Component
public class WebhookPushListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookPushListener.class);
    private static final int TEXT_MAX_LEN = 5000;

    private final HmacSigner hmacSigner;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String agentUrl;

    public WebhookPushListener(HmacSigner hmacSigner, RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                @Value("${community.event.agent-url}") String agentUrl) {
        this.hmacSigner = hmacSigner;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.agentUrl = agentUrl;
    }

    @Async
    @EventListener
    public void onPostCreated(ContentCreatedEvent.PostCreated event) {
        push(buildPayload(event, null));
    }

    @Async
    @EventListener
    public void onCommentCreated(ContentCreatedEvent.CommentCreated event) {
        push(buildPayload(event, event.getTargetType()));
    }

    @Async
    @EventListener
    public void onShareCreated(ContentCreatedEvent.ShareCreated event) {
        push(buildPayload(event, null));
    }

    private EventPayload buildPayload(ContentCreatedEvent event, String targetType) {
        String rawText = event.getContentText() == null ? "" : event.getContentText();
        boolean truncated = rawText.length() > TEXT_MAX_LEN;
        String text = truncated ? rawText.substring(0, TEXT_MAX_LEN) : rawText;

        String authorId = "user_" + event.getAuthorId();
        String contentId = event.getContentType() + "_" + event.getContentId();

        return new EventPayload(
                contentId,
                event.getContentType(),
                text,
                authorId,
                EventPayload.Metadata.created(targetType, truncated));
    }

    private void push(EventPayload payload) {
        try {
            String traceId = "wf-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            long timestamp = System.currentTimeMillis() / 1000;
            String body = objectMapper.writeValueAsString(payload);
            String signature = hmacSigner.sign(timestamp, body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Steward-Signature", "sha256=" + signature);
            headers.set("X-Steward-Timestamp", String.valueOf(timestamp));
            headers.set("X-Trace-Id", traceId);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            var response = restTemplate.postForEntity(agentUrl, request, Map.class);

            int status = response.getStatusCode().value();
            if (status == 202) {
                log.info("Webhook 推送成功: contentId={}, traceId={}, status={}", payload.contentId(), traceId, status);
            } else if (status == 422) {
                // 422 = 契约 bug 信号，记 ERROR 上报 Agent PM
                log.error("Webhook 422 契约违反: contentId={}, traceId={}, body={}", payload.contentId(), traceId, body);
            } else {
                log.warn("Webhook 推送非预期状态码: contentId={}, status={}, traceId={}", payload.contentId(), status, traceId);
            }
        } catch (Exception e) {
            // 401/5xx/超时：仅记 WARN，不重试，不阻塞主流程
            log.warn("Webhook 推送失败(不阻塞): contentId={}, contentType={}, error={}",
                    payload.contentId(), payload.contentType(), e.getMessage());
        }
    }
}

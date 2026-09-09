package com.community.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.community.event.dto.EventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookPushListenerTest {

    private static final String TEST_SECRET = "test-secret-for-hmac-signing-32byte";

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HmacSigner hmacSigner;
    private WebhookPushListener listener;

    @BeforeEach
    void setup() {
        hmacSigner = new HmacSigner(TEST_SECRET);
        listener = new WebhookPushListener(hmacSigner, restTemplate, objectMapper,
                "http://172.25.41.137:18000/api/v1/events");
    }

    @Test
    void postCreatedPublishesEventAndPushes() throws Exception {
        listener.onPostCreated(new ContentCreatedEvent.PostCreated(this, 101, "测试帖子正文", 55));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeastOnce()).postForEntity(
                org.mockito.ArgumentMatchers.anyString(), captor.capture(), org.mockito.ArgumentMatchers.any());

        HttpEntity<String> req = captor.getValue();
        assertThat(req.getHeaders().getFirst("X-Steward-Signature")).startsWith("sha256=");
        assertThat(req.getHeaders().getFirst("X-Steward-Timestamp")).isNotBlank();

        EventPayload payload = objectMapper.readValue(req.getBody(), EventPayload.class);
        assertThat(payload.contentId()).isEqualTo("post_101");
        assertThat(payload.contentType()).isEqualTo("post");
        assertThat(payload.contentText()).isEqualTo("测试帖子正文");
        assertThat(payload.authorId()).isEqualTo("user_55");
        assertThat(payload.metadata().action()).isEqualTo("created");
        assertThat(payload.metadata().source()).isEqualTo("jikezhan");
    }

    @Test
    void commentCreatedPublishesTargetType() throws Exception {
        listener.onCommentCreated(new ContentCreatedEvent.CommentCreated(this, 202, "评论内容", 66, "post"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeastOnce()).postForEntity(
                org.mockito.ArgumentMatchers.anyString(), captor.capture(), org.mockito.ArgumentMatchers.any());

        EventPayload payload = objectMapper.readValue(captor.getValue().getBody(), EventPayload.class);
        assertThat(payload.contentType()).isEqualTo("comment");
        assertThat(payload.metadata().targetType()).isEqualTo("post");
    }

    @Test
    void shareCreatedPublishesShareType() throws Exception {
        listener.onShareCreated(new ContentCreatedEvent.ShareCreated(this, 303, "分享项目介绍", 77));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeastOnce()).postForEntity(
                org.mockito.ArgumentMatchers.anyString(), captor.capture(), org.mockito.ArgumentMatchers.any());

        EventPayload payload = objectMapper.readValue(captor.getValue().getBody(), EventPayload.class);
        assertThat(payload.contentId()).isEqualTo("share_303");
        assertThat(payload.contentType()).isEqualTo("share");
    }

    @Test
    void longTextIsTruncatedAndMarkedTruncated() throws Exception {
        String longText = "a".repeat(6000);
        listener.onPostCreated(new ContentCreatedEvent.PostCreated(this, 404, longText, 88));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeastOnce()).postForEntity(
                org.mockito.ArgumentMatchers.anyString(), captor.capture(), org.mockito.ArgumentMatchers.any());

        EventPayload payload = objectMapper.readValue(captor.getValue().getBody(), EventPayload.class);
        assertThat(payload.contentText()).hasSize(5000);
        assertThat(payload.metadata().truncated()).isTrue();
    }

    /** F1：422 应走 HttpStatusCodeException 异常路径（REST 默认行为），不是 status 分支。 */
    @Test
    void status422ThrowsAndLogsAsContractBug() {
        org.mockito.Mockito.when(restTemplate.postForEntity(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                        "Unprocessable Entity",
                        new HttpHeaders(),
                        "schema invalid".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8));

        // 不应抛异常（推送失败不阻塞）
        listener.onPostCreated(new ContentCreatedEvent.PostCreated(this, 501, "422测试", 99));
        verify(restTemplate, atLeastOnce()).postForEntity(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /** F1：连接/读取超时走 ResourceAccessException → WARN 吞掉，不向外抛。 */
    @Test
    void connectTimeoutIsSwallowedAndDoesNotPropagate() {
        org.mockito.Mockito.when(restTemplate.postForEntity(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResourceAccessException("Connect timed out"));

        listener.onPostCreated(new ContentCreatedEvent.PostCreated(this, 502, "超时测试", 100));
        verify(restTemplate, atLeastOnce()).postForEntity(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signatureIsValidAndConsistent() {
        long timestamp = 1700000000L;
        String body = "{\"contentId\":\"post_101\"}";
        String sig1 = hmacSigner.sign(timestamp, body);
        String sig2 = hmacSigner.sign(timestamp, body);
        assertThat(sig1).isEqualTo(sig2);
        assertThat(sig1).hasSize(64);
    }
}

package com.community.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-BE-1 根因钉死：Webhook 目标连接超时时，同事件流上的站内通知不得被默认 @Async 线程池饿死。
 *
 * <p>手段：把 agent-url 指到 RFC5737 TEST-NET-1（公网黑洞）。无 RestTemplate 超时会挂死线程池；
 * 有 connect 2s 超时则走 ResourceAccessException（F1 WARN），通知应在秒级落库。
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookTimeoutNotifyIsolationTest {

    private static final String PREFIX = "tbe1_";

    @DynamicPropertySource
    static void blackholeWebhook(DynamicPropertyRegistry registry) {
        registry.add("community.event.agent-url", () -> "http://192.0.2.1:9/api/v1/events");
        registry.add("community.event.connect-timeout-ms", () -> "2000");
        registry.add("community.event.read-timeout-ms", () -> "3000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String tokenB;
    private long userIdA;
    private long postId;

    @BeforeEach
    void init() throws Exception {
        long sfx = System.nanoTime();
        tokenA = register(PREFIX + "a" + sfx, PREFIX + "a" + sfx + "@test.local");
        tokenB = register(PREFIX + "b" + sfx, PREFIX + "b" + sfx + "@test.local");
        userIdA = jdbcTemplate.queryForObject(
                "SELECT id FROM user WHERE username = ?", Long.class, PREFIX + "a" + sfx);
        postId = createPost(tokenA, "T-BE-1 超时隔离帖");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM comment WHERE parent_id IS NOT NULL");
        jdbcTemplate.update(
                "DELETE FROM notification WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM comment WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void webhookConnectTimeoutDoesNotStarveReplyNotification() throws Exception {
        long top = createComment(tokenA, postId, "楼主", null);
        long started = System.nanoTime();
        createComment(tokenB, postId, "路人回帖触发 webhook+notify", top);
        waitForNotification(userIdA, 1);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

        String type = jdbcTemplate.queryForObject(
                "SELECT type FROM notification WHERE user_id = ?", String.class, userIdA);
        assertThat(type).isEqualTo("REPLY");
        // 有 2s connect 超时：应远快于 OS 级 TCP 黑洞（数十秒）；5s 内落库即证明未饿死
        assertThat(elapsedMs).isLessThan(5_000L);
    }

    private void waitForNotification(long userId, int expectedCount) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Long actual = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM notification WHERE user_id = ?", Long.class, userId);
            if (actual != null && actual >= expectedCount) {
                return;
            }
            Thread.sleep(300);
        }
        Long finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE user_id = ?", Long.class, userId);
        assertThat(finalCount).isGreaterThanOrEqualTo((long) expectedCount);
    }

    private String register(String username, String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", email, "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    private long createPost(String token, String title) throws Exception {
        Long catId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug='java'", Long.class);
        String body = objectMapper.writeValueAsString(Map.of(
                "categoryId", catId, "title", title, "contentMd", "内容"));
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private long createComment(String token, long targetPostId, String content, Long parentId)
            throws Exception {
        Map<String, Object> payload = parentId == null
                ? Map.of("contentMd", content)
                : Map.of("contentMd", content, "parentId", parentId);
        String resp = mockMvc.perform(post("/api/v1/posts/" + targetPostId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }
}

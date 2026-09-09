package com.community.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验收标准（Task 20）：
 * - 回帖/点赞产生通知
 * - 自己赞自己/自己回自己不产生通知（防自嗨）
 * - 通知列表/未读数/已读标记
 * - 模块间零代码依赖（只经事件）
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotifyIntegrationTest {

    private static final String TEST_USER_PREFIX = "t20_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String tokenB;
    private long userIdA;
    private long userIdB;
    private long postId;

    @BeforeEach
    void init() throws Exception {
        long sfx = System.nanoTime();
        tokenA = registerAndGetToken(TEST_USER_PREFIX + "a" + sfx, TEST_USER_PREFIX + "a" + sfx + "@test.local");
        tokenB = registerAndGetToken(TEST_USER_PREFIX + "b" + sfx, TEST_USER_PREFIX + "b" + sfx + "@test.local");
        userIdA = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, TEST_USER_PREFIX + "a" + sfx);
        userIdB = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, TEST_USER_PREFIX + "b" + sfx);
        postId = createPost(tokenA, "T20 通知测试帖");
    }

    @AfterEach
    void cleanup() {
        // 先清所有孤儿楼中楼评论（跨用户，防止外键阻塞）
        jdbcTemplate.update("DELETE FROM comment WHERE parent_id IS NOT NULL");
        jdbcTemplate.update(
                "DELETE FROM notification WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM comment WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM user_like WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private String registerAndGetToken(String username, String email) throws Exception {
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
        Long catId = jdbcTemplate.queryForObject("SELECT id FROM post_category WHERE slug='java'", Long.class);
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

    /**
     * F5：异步断言必须轮询等待通知落库——@Async 事件消费与测试执行在不同时序。
     * T-BE-1：上限 10 秒（300ms × 30 轮）——CI 容器异步调度+DB 写入比本地慢，原 3s 不够。
     */
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

    @Test
    void replyByOtherUserCreatesNotification() throws Exception {
        long topComment = createComment(tokenA, postId, "A 的帖子");
        createComment(tokenB, postId, "B 的回帖", topComment);
        // F5：轮询等待异步通知落库（T-BE-1：上限 10s）
        waitForNotification(userIdA, 1);
        String type = jdbcTemplate.queryForObject(
                "SELECT type FROM notification WHERE user_id = ?", String.class, userIdA);
        assertThat(type).isEqualTo("REPLY");
    }

    @Test
    void selfReplyDoesNotCreateNotification() throws Exception {
        long topComment = createComment(tokenA, postId, "A 的帖子");
        createComment(tokenA, postId, "A 自己回自己", topComment);
        long notifCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE user_id = ?", Long.class, userIdA);
        assertThat(notifCount).isEqualTo(0L);
    }

    @Test
    void likeByOtherUserCreatesNotification() throws Exception {
        mockMvc.perform(post("/api/v1/likes/POST/" + postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
        // F5：轮询等待异步通知落库
        waitForNotification(userIdA, 1);
        String type = jdbcTemplate.queryForObject(
                "SELECT type FROM notification WHERE user_id = ?", String.class, userIdA);
        assertThat(type).isEqualTo("LIKE");
    }

    @Test
    void selfLikeDoesNotCreateNotification() throws Exception {
        mockMvc.perform(post("/api/v1/likes/POST/" + postId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        Thread.sleep(500); // 等异步消费确认不产生通知
        long notifCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE user_id = ?", Long.class, userIdA);
        assertThat(notifCount).isEqualTo(0L);
    }

    @Test
    void notificationListAndUnreadCount() throws Exception {
        // B 回帖产生通知
        long topComment = createComment(tokenA, postId, "帖子");
        createComment(tokenB, postId, "B 回帖", topComment);
        // F5：轮询等待
        waitForNotification(userIdA, 1);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].type").value("REPLY"));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void markReadAndMarkAllRead() throws Exception {
        long topComment = createComment(tokenA, postId, "帖子");
        createComment(tokenB, postId, "B 回帖", topComment);

        // 标记全部已读
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    private long createComment(String token, long postId, String content) throws Exception {
        return createComment(token, postId, content, null);
    }

    private long createComment(String token, long postId, String content, Long parentId) throws Exception {
        Map<String, Object> body = parentId == null
                ? Map.of("contentMd", content)
                : Map.of("contentMd", content, "parentId", parentId);
        String resp = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }
}

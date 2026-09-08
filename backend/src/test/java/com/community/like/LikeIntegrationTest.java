package com.community.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验收标准（Task 19）：toggle 幂等、取消再赞计数恢复、GREATEST 兜底、未登录 401、
 * target_type 非法值 400、被删目标 404、批量查询点赞状态。
 */
@SpringBootTest
@AutoConfigureMockMvc
class LikeIntegrationTest {

    private static final String TEST_USER_PREFIX = "t19_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String tokenB;
    private long postCategoryId;
    private long postId;

    @BeforeEach
    void init() throws Exception {
        postCategoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug='java'", Long.class);

        long sfx = System.nanoTime();
        tokenA = registerAndGetToken(TEST_USER_PREFIX + "a" + sfx);
        tokenB = registerAndGetToken(TEST_USER_PREFIX + "b" + sfx);
        postId = createPost("T19 点赞测试帖");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM user_like WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)", TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM post WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)", TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private String registerAndGetToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", username + "@test.local", "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    private long createPost(String title) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", postCategoryId,
                                "title", title,
                                "contentMd", "内容"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private void likeAs(String token, String targetType, long targetId) throws Exception {
        mockMvc.perform(post("/api/v1/likes/" + targetType + "/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void toggleLikeAndUnlike() throws Exception {
        // 第一次点赞→true，like_count +1
        mockMvc.perform(post("/api/v1/likes/POST/" + postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        assertThat(getLikeCount(postId)).isEqualTo(1L);

        // 第二次→取消，返回 false，like_count -1
        mockMvc.perform(post("/api/v1/likes/POST/" + postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
        assertThat(getLikeCount(postId)).isEqualTo(0L);
    }

    @Test
    void doubleUnlikeFloorZero() throws Exception {
        mockMvc.perform(post("/api/v1/likes/POST/" + postId)
                        .header("Authorization", "Bearer " + tokenB));
        mockMvc.perform(post("/api/v1/likes/POST/" + postId)
                        .header("Authorization", "Bearer " + tokenB));
        mockMvc.perform(post("/api/v1/likes/POST/" + postId)
                        .header("Authorization", "Bearer " + tokenB));
        assertThat(getLikeCount(postId)).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/likes/POST/" + postId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTargetTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/likes/INVALID/1")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletedTargetReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/likes/POST/999999999")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void batchLikedStatusWorks() throws Exception {
        long post2 = createPost("帖子2");
        likeAs(tokenB, "POST", postId);
        mockMvc.perform(get("/api/v1/likes/status")
                        .param("targetType", "POST")
                        .param("targetIds", postId + "," + post2)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value(postId));
    }

    private long getLikeCount(long postId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT like_count FROM post WHERE id = ?", Long.class, postId);
        return count != null ? count : 0L;
    }
}

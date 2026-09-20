package com.community.steward;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * T-DATA-1：管家只读数据 API 三路径 + 脱敏断言。
 */
@SpringBootTest
@AutoConfigureMockMvc
class StewardDataApiIntegrationTest {

    private static final String PREFIX = "td1_";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminUsername;
    private String userUsername;
    private String subjectUsername;
    private String adminToken;
    private String userToken;
    private long subjectUserId;
    private long postId;

    @BeforeEach
    void setUp() throws Exception {
        long s = System.nanoTime();
        adminUsername = PREFIX + "admin" + s;
        userUsername = PREFIX + "user" + s;
        subjectUsername = PREFIX + "sub" + s;
        adminToken = registerAndGetAccess(adminUsername);
        userToken = registerAndGetAccess(userUsername);
        registerAndGetAccess(subjectUsername);
        jdbcTemplate.update("UPDATE user SET role = 'ADMIN' WHERE username = ?", adminUsername);
        adminToken = loginAndGetAccess(adminUsername);
        subjectUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM user WHERE username = ?", Long.class, subjectUsername);
        String subjectToken = loginAndGetAccess(subjectUsername);
        postId = createPost(subjectToken, "tdata-post-" + s);
        createComment(subjectToken, postId, "tdata-comment-body");
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update(
                "DELETE FROM comment WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void adminCanReadPostsCommentsStatsAndPenaltiesPlaceholder() throws Exception {
        String postsJson = performGet("/api/v1/admin/data/users/user_" + subjectUserId + "/posts",
                adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].contentId").value("post_" + postId))
                .andExpect(jsonPath("$.data.records[0].userId").value("user_" + subjectUserId))
                .andExpect(jsonPath("$.data.records[0].username").value(subjectUsername))
                .andExpect(jsonPath("$.data.records[0].contentType").value("POST"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertNoSensitiveFields(postsJson);

        String commentsJson = performGet(
                "/api/v1/admin/data/users/" + subjectUserId + "/comments", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].contentId", Matchers.startsWith("comment_")))
                .andExpect(jsonPath("$.data.records[0].targetId").value("post_" + postId))
                .andExpect(jsonPath("$.data.records[0].contentType").value("COMMENT"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertNoSensitiveFields(commentsJson);

        String statsJson = performGet("/api/v1/admin/data/stats", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userCount").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.postCount").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.penaltyCount").value(0))
                .andExpect(jsonPath("$.data.penaltyNote", Matchers.containsString("占位")))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertNoSensitiveFields(statsJson);

        String penaltiesJson = performGet("/api/v1/admin/data/penalties", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.placeholder").value(true))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertNoSensitiveFields(penaltiesJson);
    }

    @Test
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/data/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void normalUserForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/data/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    private void assertNoSensitiveFields(String json) {
        String lower = json.toLowerCase();
        org.junit.jupiter.api.Assertions.assertFalse(lower.contains("email"), json);
        org.junit.jupiter.api.Assertions.assertFalse(lower.contains("password"), json);
        org.junit.jupiter.api.Assertions.assertFalse(lower.contains("passwordhash"), json);
        org.junit.jupiter.api.Assertions.assertFalse(lower.contains("accesstoken"), json);
        org.junit.jupiter.api.Assertions.assertFalse(lower.contains("refreshtoken"), json);
        org.junit.jupiter.api.Assertions.assertFalse(lower.contains("tokenversion"), json);
        org.junit.jupiter.api.Assertions.assertFalse(lower.contains("@test.local"), json);
    }

    private ResultActions performGet(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    private long createPost(String token, String title) throws Exception {
        long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug = ?", Long.class, "java");
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId,
                                "title", title,
                                "contentMd", "tdata body"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private void createComment(String token, long postId, String body) throws Exception {
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contentMd", body))))
                .andExpect(status().isOk());
    }

    private String registerAndGetAccess(String username) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@test.local",
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    private String loginAndGetAccess(String username) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(resp).path("data");
        return data.path("accessToken").asText();
    }
}

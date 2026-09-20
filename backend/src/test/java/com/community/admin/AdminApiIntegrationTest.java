package com.community.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
 * T-M4-1：管理端删帖/禁言三路径（ADMIN 成功 / USER 403 / 未登录 401）
 * + 封禁后 login/refresh 双拒 + 删帖后详情 404。
 * T-ADMIN-FEATURE：加精/置顶成功·幂等·详情可见 + 403/401。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiIntegrationTest {

    private static final String PREFIX = "m41_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminUsername;
    private String userUsername;
    private String targetUsername;
    private String adminToken;
    private String userToken;
    private String targetPassword = "password123";
    private long targetUserId;

    @BeforeEach
    void setUp() throws Exception {
        long s = System.nanoTime();
        adminUsername = PREFIX + "admin" + s;
        userUsername = PREFIX + "user" + s;
        targetUsername = PREFIX + "target" + s;
        adminToken = registerAndGetAccess(adminUsername);
        userToken = registerAndGetAccess(userUsername);
        registerAndGetAccess(targetUsername);
        jdbcTemplate.update("UPDATE user SET role = 'ADMIN' WHERE username = ?", adminUsername);
        // 提权后须重新登录，JWT 才带 ROLE_ADMIN
        adminToken = loginAndGetAccess(adminUsername, targetPassword);
        targetUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM user WHERE username = ?", Long.class, targetUsername);
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void adminDeletePostSucceedsAndDetail404() throws Exception {
        long postId = createPost(userToken, "admin-delete-me");

        mockMvc.perform(delete("/api/v1/admin/posts/" + postId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void userDeletePostForbidden() throws Exception {
        long postId = createPost(userToken, "user-cannot-admin-delete");

        mockMvc.perform(delete("/api/v1/admin/posts/" + postId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void anonymousDeletePostUnauthorized() throws Exception {
        long postId = createPost(userToken, "anon-cannot-admin-delete");

        mockMvc.perform(delete("/api/v1/admin/posts/" + postId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void adminBanUserThenLoginAndRefreshRejected() throws Exception {
        JsonNode tokens = loginAndGetTokens(targetUsername, targetPassword);
        String refreshToken = tokens.path("refreshToken").asText();

        mockMvc.perform(put("/api/v1/admin/users/" + targetUserId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", 1,
                                "reason", "spam"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", targetUsername,
                                "password", targetPassword))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("账号已被封禁"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("账号已被封禁"));
    }

    @Test
    void userBanForbiddenAndAnonymousUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/" + targetUserId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(put("/api/v1/admin/users/" + targetUserId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", 1))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void adminUnbanRestoresLogin() throws Exception {
        jdbcTemplate.update("UPDATE user SET status = 1 WHERE id = ?", targetUserId);

        mockMvc.perform(put("/api/v1/admin/users/" + targetUserId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", 0))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", targetUsername,
                                "password", targetPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    // ---------- T-ADMIN-FEATURE：加精 / 置顶 ----------

    @Test
    void adminFeatureAndPinSucceedAndReflectOnDetail() throws Exception {
        long postId = createPost(userToken, "admin-feature-pin");

        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/featured")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("featured", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/pinned")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinned", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFeatured").value(1))
                .andExpect(jsonPath("$.data.isPinned").value(1));

        // 幂等：重复加精/置顶不炸
        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/featured")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("featured", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/pinned")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinned", 1))))
                .andExpect(status().isOk());

        // 取消
        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/featured")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("featured", 0))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/pinned")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinned", 0))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFeatured").value(0))
                .andExpect(jsonPath("$.data.isPinned").value(0));
    }

    @Test
    void userFeatureForbiddenAndAnonymousUnauthorized() throws Exception {
        long postId = createPost(userToken, "user-cannot-feature");

        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/featured")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("featured", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/pinned")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinned", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/featured")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("featured", 1))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(put("/api/v1/admin/posts/" + postId + "/pinned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinned", 1))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void featureMissingPostReturns404() throws Exception {
        mockMvc.perform(put("/api/v1/admin/posts/999999991/featured")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("featured", 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
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
                                "contentMd", "body"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private String registerAndGetAccess(String username) throws Exception {
        return registerAndGetTokens(username).path("accessToken").asText();
    }

    private JsonNode registerAndGetTokens(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@test.local",
                "password", targetPassword));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data");
    }

    private String loginAndGetAccess(String username, String password) throws Exception {
        return loginAndGetTokens(username, password).path("accessToken").asText();
    }

    private JsonNode loginAndGetTokens(String username, String password) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data");
    }
}

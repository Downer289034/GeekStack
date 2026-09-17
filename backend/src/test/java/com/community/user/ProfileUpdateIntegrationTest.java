package com.community.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-PROFILE-BE 验收：PUT /me 本人改；未登录 401；他人令牌无法改写本人资料（隔离）；
 * 改密后旧 refresh 失效；OAuth 无密用户可首设密码。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileUpdateIntegrationTest {

    private static final String PREFIX = "tprof_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM user_oauth_binding WHERE user_id IN ("
                + "SELECT id FROM user WHERE username LIKE ?)", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    private JsonNode register(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@test.local",
                "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data");
    }

    @Test
    void ownerCanUpdateMe() throws Exception {
        String username = PREFIX + "a_" + System.nanoTime();
        String access = register(username).path("accessToken").asText();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "新昵称",
                                "bio", "简介内容",
                                "avatarUrl", "https://cdn.example/a.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.bio").value("简介内容"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example/a.png"))
                .andExpect(jsonPath("$.data.hasPassword").value(true))
                .andExpect(jsonPath("$.data.oauthBindings.github").value(false))
                .andExpect(jsonPath("$.data.oauthBindings.gitee").value(false));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.hasPassword").value(true));
    }

    @Test
    void updateMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "x"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void otherUserTokenCannotOverwriteOwnerProfile() throws Exception {
        String owner = PREFIX + "own_" + System.nanoTime();
        String other = PREFIX + "oth_" + System.nanoTime();
        String ownerAccess = register(owner).path("accessToken").asText();
        String otherAccess = register(other).path("accessToken").asText();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + ownerAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "主人昵称"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + otherAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "他人试图"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(other))
                .andExpect(jsonPath("$.data.nickname").value("他人试图"));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + ownerAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("主人昵称"));
    }

    @Test
    void changePasswordInvalidatesOldRefresh() throws Exception {
        String username = PREFIX + "pwd_" + System.nanoTime();
        JsonNode tokens = register(username);
        String access = tokens.path("accessToken").asText();
        String refresh = tokens.path("refreshToken").asText();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "password123",
                                "newPassword", "password456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        String loginBody = objectMapper.writeValueAsString(
                Map.of("username", username, "password", "password456"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void firstSetPasswordForNullHashUser() throws Exception {
        // 注册拿令牌 → 清空 password_hash 模拟 OAuth 用户 → 首设免验旧密码
        String username = PREFIX + "nullpwd_" + System.nanoTime();
        JsonNode tokens = register(username);
        final String access = tokens.path("accessToken").asText();
        final String oldRefresh = tokens.path("refreshToken").asText();

        jdbcTemplate.update("UPDATE user SET password_hash = NULL WHERE username = ?", username);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasPassword").value(false));

        Map<String, Object> body = new HashMap<>();
        body.put("oldPassword", null);
        body.put("newPassword", "password789");
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasPassword").value(true));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", "password789"))))
                .andExpect(status().isOk());
    }

    @Test
    void wrongOldPasswordRejected() throws Exception {
        String username = PREFIX + "badold_" + System.nanoTime();
        String access = register(username).path("accessToken").asText();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "wrong-old-pw",
                                "newPassword", "password999"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("原密码错误"));
    }

    @Test
    void passwordEndpointRequiresAuth() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("newPassword", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }
}

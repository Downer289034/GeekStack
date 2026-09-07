package com.community.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验收标准（Task 7）：注册→登录→带 JWT 访问 /api/v1/users/me 成功；错误密码返回统一错误码。
 * 集成测试直连开发库（服务器 MySQL），测试用户以 t7_ 前缀创建并在用例结束后清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserAuthIntegrationTest {

    private static final String TEST_USER_PREFIX = "t7_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String username;
    private String email;
    private String password;

    @BeforeEach
    void initUniqueUser() {
        username = TEST_USER_PREFIX + System.nanoTime();
        email = username + "@test.local";
        password = "password123";
    }

    @AfterEach
    void cleanTestUsers() {
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private String registerAndGetResponse() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "email", email, "password", password));
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private JsonNode registerAndGetTokens() throws Exception {
        return objectMapper.readTree(registerAndGetResponse()).path("data");
    }

    @Test
    void registerThenLoginThenMeJourney() throws Exception {
        JsonNode tokens = registerAndGetTokens();
        String accessToken = tokens.path("accessToken").asText();
        String refreshToken = tokens.path("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("USER"));

        String loginBody = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void duplicateUsernameOrEmailReturns409() throws Exception {
        registerAndGetResponse();

        String sameUsername = objectMapper.writeValueAsString(
                Map.of("username", username, "email", "other_" + email, "password", password));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameUsername))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        String sameEmail = objectMapper.writeValueAsString(
                Map.of("username", "other_" + username, "email", email, "password", password));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameEmail))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void wrongPasswordReturns401UnifiedCode() throws Exception {
        registerAndGetResponse();

        String loginBody = objectMapper.writeValueAsString(
                Map.of("username", username, "password", "wrong-password"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void meWithoutTokenReturns401UnifiedBody() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"))
                .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    void refreshWithAccessTokenRejected() throws Exception {
        JsonNode tokens = registerAndGetTokens();

        String refreshBody = objectMapper.writeValueAsString(
                Map.of("refreshToken", tokens.path("accessToken").asText()));
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void refreshRejectedForBannedUser() throws Exception {
        // P2-3：封禁用户即使持有有效 refresh 令牌也不能续期
        JsonNode tokens = registerAndGetTokens();
        jdbcTemplate.update("UPDATE user SET status = 1 WHERE username = ?", username);

        String refreshBody = objectMapper.writeValueAsString(
                Map.of("refreshToken", tokens.path("refreshToken").asText()));
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("账号已被封禁"));
    }

    @Test
    void invalidRegisterParamsReturn400() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "email", email, "password", "123"));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }
}

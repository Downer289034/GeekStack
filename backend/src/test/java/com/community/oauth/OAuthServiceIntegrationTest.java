package com.community.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.community.common.AesGcmCipher;
import com.community.oauth.dto.OAuthUserInfo;
import com.community.oauth.service.GitHubOAuthService;
import com.community.oauth.service.GiteeOAuthService;
import com.community.oauth.service.OAuthStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 验收标准（Task 15，平台交互 mock，联调留待 OAuth App 就绪）：
 * - 未登录回调 = 第三方登录（自动注册）；已登录回调 = 绑定到当前账号；
 * - 邮箱冲突保守语义：邮箱已注册 → 409 提示"用密码登录后绑定"，不自动合并；
 * - 库中无明文 Token（一票否决项）：access_token_enc 为 AES 密文，解密可还原。
 * 测试用户以 s15_ 前缀创建并在用例结束后清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OAuthServiceIntegrationTest {

    private static final String TEST_USER_PREFIX = "s15_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AesGcmCipher cipher;

    @Autowired
    private OAuthStateService oAuthStateService;

    @MockBean
    private GitHubOAuthService gitHubOAuthService;

    @MockBean
    private GiteeOAuthService giteeOAuthService;

    private String lastUsername;

    @AfterEach
    void cleanTestData() {
        jdbcTemplate.update(
                "DELETE FROM user_oauth_binding WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private void mockGitHub(String openid, String login, String email) {
        when(gitHubOAuthService.exchangeCode(anyString())).thenReturn("gho_mock_token_" + openid);
        when(gitHubOAuthService.fetchUserInfo(anyString()))
                .thenReturn(new OAuthUserInfo(openid, login, "GH昵称_" + login, email, "http://a/img.png"));
    }

    private String codeBody(String code, String provider) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "code", code,
                "state", oAuthStateService.issue(provider)));
    }

    private String submitGithubCode(String code) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codeBody(code, "GITHUB")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        lastUsername = objectMapper.readTree(resp).path("data").path("accessToken") == null
                ? null
                : lastUsername;
        return resp;
    }

    private String tokenOf(String resp) throws Exception {
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    @Test
    void loginByGithubAutoRegistersAndStoresEncryptedToken() throws Exception {
        mockGitHub("9001", "ghuser_a", null);
        String resp = submitGithubCode("code-1");

        // 自动注册 + 返回令牌
        assertThat(tokenOf(resp)).isNotBlank();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, "ghuser_a");
        assertThat(count).isEqualTo(1);
        lastUsername = "ghuser_a";

        // 绑定行存在且 Token 为 AES 密文：无明文（一票否决项），解密可还原
        Map<String, Object> binding = jdbcTemplate.queryForMap(
                "SELECT access_token_enc, refresh_token, provider, openid FROM user_oauth_binding WHERE openid = ?",
                "9001");
        assertThat(binding.get("provider")).isEqualTo("GITHUB");
        String stored = (String) binding.get("access_token_enc");
        assertThat(stored).isNotEqualTo("gho_mock_token_9001");
        assertThat(stored).doesNotContain("gho_");
        assertThat(cipher.decrypt(stored)).isEqualTo("gho_mock_token_9001");

        // 二次登录（同 openid）不重复建账号
        submitGithubCode("code-2");
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, "ghuser_a");
        assertThat(userCount).isEqualTo(1);
    }

    @Test
    void emailConflictReturns409AndDoesNotMerge() throws Exception {
        // 先有一个本地注册账号（邮箱占用）
        String localUser = TEST_USER_PREFIX + "local";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", localUser,
                                "email", "conflict15@test.local",
                                "password", "password123"))))
                .andExpect(status().isOk());

        // GitHub 账号带同一邮箱 → 409，提示绑定
        mockGitHub("9002", "ghuser_b", "conflict15@test.local");
        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codeBody("code-3", "GITHUB")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value("该邮箱已注册，请使用密码登录后在设置中绑定GITHUB账号"));

        // 未产生新账号、未产生绑定
        Integer users = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, "ghuser_b");
        assertThat(users).isEqualTo(0);
        Integer bindings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_oauth_binding WHERE openid = ?", Integer.class, "9002");
        assertThat(bindings).isEqualTo(0);
    }

    @Test
    void loggedInCallbackBindsToCurrentUser() throws Exception {
        // 本地账号登录后带 JWT 调回调 → 绑定到该账号（而非新建账号）
        String localUser = TEST_USER_PREFIX + "binder";
        String registerResp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", localUser,
                                "email", localUser + "@test.local",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String jwt = objectMapper.readTree(registerResp).path("data").path("accessToken").asText();

        mockGitHub("9003", "ghuser_c", null);
        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codeBody("code-4", "GITHUB")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long localId = jdbcTemplate.queryForObject(
                "SELECT id FROM user WHERE username = ?", Long.class, localUser);
        Long boundUserId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM user_oauth_binding WHERE openid = ?", Long.class, "9003");
        assertThat(boundUserId).isEqualTo(localId);

        // 用户数不变（没有新建 ghuser_c）
        Integer users = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = 'ghuser_c'", Integer.class);
        assertThat(users).isEqualTo(0);
        lastUsername = localUser;
    }

    @Test
    void giteeLoginStoresEncryptedAccessAndRefresh() throws Exception {
        when(giteeOAuthService.exchangeCode(anyString()))
                .thenReturn(new GiteeOAuthService.TokenExchangeResult(
                        "gitee_mock_access", "gitee_mock_refresh", null));
        when(giteeOAuthService.fetchUserInfo(anyString()))
                .thenReturn(new OAuthUserInfo("7001", "giteeuser", "Gitee昵称", null, "http://a/g.png"));

        String resp = mockMvc.perform(post("/api/v1/oauth/GITEE/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codeBody("code-5", "GITEE")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(tokenOf(resp)).isNotBlank();

        Map<String, Object> binding = jdbcTemplate.queryForMap(
                "SELECT access_token_enc, refresh_token FROM user_oauth_binding WHERE openid = ?", "7001");
        String accessEnc = (String) binding.get("access_token_enc");
        String refreshEnc = (String) binding.get("refresh_token");
        assertThat(accessEnc).doesNotContain("gitee_mock");
        assertThat(refreshEnc).doesNotContain("gitee_mock");
        assertThat(cipher.decrypt(accessEnc)).isEqualTo("gitee_mock_access");
        assertThat(cipher.decrypt(refreshEnc)).isEqualTo("gitee_mock_refresh");
    }
}

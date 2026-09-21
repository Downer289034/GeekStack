package com.community.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.AesGcmCipher;
import com.community.oauth.entity.UserOauthBinding;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserOauthBindingMapper;
import com.community.oauth.mapper.UserRepoMapper;
import com.community.oauth.service.GitHubRepoProvider;
import com.community.oauth.service.RepoProvider;
import com.community.oauth.service.RepoSyncRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-REPO-MANUAL-SYNC-API：POST /api/v1/me/repos/sync（成功 mock Provider / 未绑定 / 限流）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManualRepoSyncIntegrationTest {

    private static final String PREFIX = "mrs_";

    @DynamicPropertySource
    static void cooldown(DynamicPropertyRegistry registry) {
        registry.add("community.oauth.repo-sync-cooldown-seconds", () -> "60");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AesGcmCipher cipher;

    @Autowired
    private UserOauthBindingMapper bindingMapper;

    @Autowired
    private UserRepoMapper repoMapper;

    @Autowired
    private RepoSyncRateLimiter rateLimiter;

    @MockBean
    private GitHubRepoProvider gitHubRepoProvider;

    private String token;
    private long userId;
    private String username;

    @BeforeEach
    void init() throws Exception {
        when(gitHubRepoProvider.provider()).thenReturn("GITHUB");
        rateLimiter.clear();

        username = PREFIX + System.nanoTime();
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "email", username + "@test.local", "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        token = objectMapper.readTree(resp).path("data").path("accessToken").asText();
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM user WHERE username = ?", Long.class, username);
    }

    @AfterEach
    void cleanup() {
        rateLimiter.clear();
        jdbcTemplate.update(
                "DELETE FROM user_repo WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM user_oauth_binding WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    private void insertGithubBinding() {
        UserOauthBinding binding = new UserOauthBinding();
        binding.setUserId(userId);
        binding.setProvider("GITHUB");
        binding.setOpenid("mrs_openid_" + userId);
        binding.setAccessTokenEnc(cipher.encrypt("mock_github_token"));
        binding.setExpiresAt(LocalDateTime.now().plusHours(2));
        bindingMapper.insert(binding);
    }

    @Test
    void syncSuccessWithMockedProvider() throws Exception {
        insertGithubBinding();
        when(gitHubRepoProvider.fetchPublicRepos(anyString())).thenReturn(List.of(
                new RepoProvider.RemoteRepo("alice/demo", "https://github.com/alice/demo", "demo", 7L)));

        mockMvc.perform(post("/api/v1/me/repos/sync?provider=GITHUB")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("GITHUB"))
                .andExpect(jsonPath("$.data.syncedCount").value(1));

        List<UserRepo> repos = repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, "GITHUB"));
        assertThat(repos).hasSize(1);
        assertThat(repos.get(0).getRepoFullName()).isEqualTo("alice/demo");
        assertThat(repos.get(0).getStars()).isEqualTo(7L);
    }

    @Test
    void syncBodyProviderWorks() throws Exception {
        insertGithubBinding();
        when(gitHubRepoProvider.fetchPublicRepos(anyString())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/me/repos/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"github\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("GITHUB"))
                .andExpect(jsonPath("$.data.syncedCount").value(0));
    }

    @Test
    void unboundProviderReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/me/repos/sync?provider=GITHUB")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("未绑定")));
    }

    @Test
    void missingProviderReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/me/repos/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void anonymousReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/me/repos/sync?provider=GITHUB"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitReturns429() throws Exception {
        insertGithubBinding();
        when(gitHubRepoProvider.fetchPublicRepos(anyString())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/me/repos/sync?provider=GITHUB")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/me/repos/sync?provider=GITHUB")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(42900))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("频繁")));
    }

    @Test
    void remoteFailurePreservesOldSnapshotAndReturns503() throws Exception {
        insertGithubBinding();
        UserRepo old = new UserRepo();
        old.setUserId(userId);
        old.setProvider("GITHUB");
        old.setRepoFullName("keep/old");
        old.setRepoUrl("https://github.com/keep/old");
        old.setStars(1L);
        old.setSyncedAt(LocalDateTime.now().minusDays(1));
        repoMapper.insert(old);

        when(gitHubRepoProvider.fetchPublicRepos(anyString()))
                .thenThrow(new RuntimeException("upstream down"));

        mockMvc.perform(post("/api/v1/me/repos/sync?provider=GITHUB")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(50300))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("保留")));

        List<UserRepo> repos = repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, "GITHUB"));
        assertThat(repos).hasSize(1);
        assertThat(repos.get(0).getRepoFullName()).isEqualTo("keep/old");
    }
}

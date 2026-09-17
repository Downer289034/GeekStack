package com.community.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.community.oauth.dto.OAuthUserInfo;
import com.community.oauth.service.GitHubOAuthService;
import com.community.oauth.service.GiteeOAuthService;
import com.community.oauth.service.OAuthStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * T-SEC-1-BE：OAuth state 四路径（缺失/篡改/过期/重放 → 401）+ 正常路径不受影响。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OAuthStateIntegrationTest {

    private static final String PREFIX = "ssec_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OAuthStateService oAuthStateService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private GitHubOAuthService gitHubOAuthService;

    @MockBean
    private GiteeOAuthService giteeOAuthService;

    @AfterEach
    void clean() {
        jdbcTemplate.update(
                "DELETE FROM user_oauth_binding WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    private void mockGitHub(String openid, String login) {
        when(gitHubOAuthService.exchangeCode(anyString())).thenReturn("gho_sec_" + openid);
        when(gitHubOAuthService.fetchUserInfo(anyString()))
                .thenReturn(new OAuthUserInfo(openid, login, "昵称", null, "http://a.png"));
    }

    @Test
    void loginRedirectIncludesStateAndHappyPathSucceeds() throws Exception {
        mockGitHub("8101", PREFIX + "ok");
        MvcResult redirect = mockMvc.perform(get("/api/v1/oauth/login/GITHUB"))
                .andExpect(status().isFound())
                .andReturn();
        String location = redirect.getResponse().getHeader("Location");
        assertThat(location).contains("state=");
        String state = location.substring(location.indexOf("state=") + 6);
        if (state.contains("&")) {
            state = state.substring(0, state.indexOf('&'));
        }

        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "sec-ok",
                                "state", state))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void missingStateReturns401() throws Exception {
        mockGitHub("8102", PREFIX + "miss");
        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "sec-miss"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"sec-miss2\",\"state\":\"\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void tamperedStateReturns401() throws Exception {
        mockGitHub("8103", PREFIX + "tamp");
        String state = oAuthStateService.issue("GITHUB");
        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "sec-tamp",
                                "state", state + "dead"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void expiredStateReturns401() throws Exception {
        mockGitHub("8104", PREFIX + "exp");
        String state = oAuthStateService.issue("GITHUB");
        redisTemplate.delete(OAuthStateService.redisKey(state));

        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "sec-exp",
                                "state", state))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void replayStateReturns401() throws Exception {
        mockGitHub("8105", PREFIX + "replay");
        String state = oAuthStateService.issue("GITHUB");
        String body = objectMapper.writeValueAsString(Map.of("code", "sec-replay-1", "state", state));

        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/oauth/GITHUB/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "sec-replay-2",
                                "state", state))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void giteeStateHappyPath() throws Exception {
        when(giteeOAuthService.exchangeCode(anyString()))
                .thenReturn(new GiteeOAuthService.TokenExchangeResult("ga", "gr", null));
        when(giteeOAuthService.fetchUserInfo(anyString()))
                .thenReturn(new OAuthUserInfo("8201", PREFIX + "gitee", "G", null, null));

        String state = oAuthStateService.issue("GITEE");
        String resp = mockMvc.perform(post("/api/v1/oauth/GITEE/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "gitee-sec",
                                "state", state))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(resp).path("data").path("accessToken").asText()).isNotBlank();
    }
}

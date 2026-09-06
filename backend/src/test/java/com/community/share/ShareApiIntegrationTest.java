package com.community.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
 * 验收标准（Task 9）：未登录发布返回 401；字段校验完整（含 URL 格式）；编辑仅限作者本人。
 * 集成测试直连开发库，测试用户以 s9_ 前缀创建并在用例结束后清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShareApiIntegrationTest {

    private static final String TEST_USER_PREFIX = "s9_";
    private static final long NONEXISTENT_ID = 999_999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String userA;
    private String userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void registerTwoUsers() throws Exception {
        long suffix = System.nanoTime();
        userA = TEST_USER_PREFIX + "a" + suffix;
        userB = TEST_USER_PREFIX + "b" + suffix;
        tokenA = registerAndGetToken(userA);
        tokenB = registerAndGetToken(userB);
    }

    @AfterEach
    void cleanTestData() {
        jdbcTemplate.update(
                "DELETE FROM project_share WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private String registerAndGetToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@test.local",
                "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    private Map<String, Object> validPayload() {
        return Map.of(
                "title", "开源项目 A",
                "summary", "一个用于测试的项目",
                "contentMd", "# 简介\n\n这是 **Markdown** 内容",
                "repoUrl", "https://github.com/example/repo",
                "demoUrl", "https://demo.example.com",
                "tags", List.of("Java", "Spring"));
    }

    private long createShareAsUserA() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.tags[0]").value("Java"))
                .andExpect(jsonPath("$.data.repoUrl").value("https://github.com/example/repo"))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(resp).path("data");
        assertThat(data.path("id").asLong()).isPositive();
        return data.path("id").asLong();
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void createByAuthorSucceedsWithTagsAndUrls() throws Exception {
        createShareAsUserA();
    }

    @Test
    void invalidPayloadReturns400() throws Exception {
        // 缺 title
        Map<String, Object> missingTitle = Map.of(
                "summary", "s", "contentMd", "c");
        mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        // 非法 repoUrl 格式
        Map<String, Object> badUrl = Map.of(
                "title", "t", "summary", "s", "contentMd", "c", "repoUrl", "not-a-url");
        mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void updateOnlyByOwner() throws Exception {
        long shareId = createShareAsUserA();

        // 作者本人编辑成功
        Map<String, Object> updated = Map.of(
                "title", "更新后的标题",
                "summary", "更新后的简介",
                "contentMd", "更新内容");
        mockMvc.perform(put("/api/v1/shares/" + shareId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("更新后的标题"));

        // 他人编辑 → 403
        mockMvc.perform(put("/api/v1/shares/" + shareId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 不存在 → 404
        mockMvc.perform(put("/api/v1/shares/" + NONEXISTENT_ID)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void hideOnlyByOwner() throws Exception {
        long shareId = createShareAsUserA();

        // 他人下架 → 403
        mockMvc.perform(post("/api/v1/shares/" + shareId + "/hide")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 作者本人下架成功
        mockMvc.perform(post("/api/v1/shares/" + shareId + "/hide")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
    }
}

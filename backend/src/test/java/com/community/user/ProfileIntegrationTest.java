package com.community.user;

import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * 验收标准（Task 13）：访问任意用户主页能看到其分享与发帖列表；未注册用户名返回 404。
 * 口径：他人视角仅 PUBLISHED；本人视角含 HIDDEN；列表各限量 10。
 * 测试用户以 s13_ 前缀创建并在用例结束后清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileIntegrationTest {

    private static final String TEST_USER_PREFIX = "s13_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String username;
    private String token;
    private long shareId;

    @BeforeEach
    void initUser() throws Exception {
        username = TEST_USER_PREFIX + System.nanoTime();
        token = registerAndGetToken(username);
    }

    @AfterEach
    void cleanTestData() {
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM project_share WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private String registerAndGetToken(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", name,
                "email", name + "@test.local",
                "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    private long createShare(String title, boolean hidden) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "summary", "简介 " + title, "contentMd", "内容"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        long id = objectMapper.readTree(resp).path("data").path("id").asLong();
        if (hidden) {
            mockMvc.perform(post("/api/v1/shares/" + id + "/hide")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        return id;
    }

    private long createPost(String title) throws Exception {
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug='java'", Long.class);
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId, "title", title, "contentMd", "正文"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    @Test
    void profileAggregatesSharesAndPosts() throws Exception {
        shareId = createShare("主页可见项目", false);
        createPost("主页可见帖子");

        String resp = mockMvc.perform(get("/api/v1/users/" + username + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.username").value(username))
                .andExpect(jsonPath("$.data.profile.email").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(resp).path("data");
        assertThat(data.path("shares").size()).isEqualTo(1);
        assertThat(data.path("shares").get(0).path("title").asText()).isEqualTo("主页可见项目");
        assertThat(data.path("posts").size()).isEqualTo(1);
        assertThat(data.path("posts").get(0).path("title").asText()).isEqualTo("主页可见帖子");
    }

    @Test
    void hiddenShareVisibleOnlyToOwner() throws Exception {
        createShare("公开项目", false);
        long hiddenId = createShare("隐藏项目", true);

        // 匿名（他人）视角：仅公开项目
        String anonResp = mockMvc.perform(get("/api/v1/users/" + username + "/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode anonShares = objectMapper.readTree(anonResp).path("data").path("shares");
        assertThat(anonShares.size()).isEqualTo(1);
        assertThat(anonShares.get(0).path("title").asText()).isEqualTo("公开项目");

        // 本人视角：含 HIDDEN
        String ownerResp = mockMvc.perform(get("/api/v1/users/" + username + "/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode ownerShares = objectMapper.readTree(ownerResp).path("data").path("shares");
        assertThat(ownerShares.size()).isEqualTo(2);
        assertThat(ownerShares.toString()).contains("隐藏项目");
        assertThat(hiddenId).isPositive();
    }

    @Test
    void listLimitedTo10() throws Exception {
        for (int i = 1; i <= 12; i++) {
            createShare("项目" + i, false);
        }
        String resp = mockMvc.perform(get("/api/v1/users/" + username + "/profile"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(resp).path("data").path("shares").size()).isEqualTo(10);
    }

    @Test
    void unknownUsernameReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/绝对不存在的主角_xyz/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }
}

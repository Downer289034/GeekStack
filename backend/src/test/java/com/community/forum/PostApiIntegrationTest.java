package com.community.forum;

import static org.assertj.core.api.Assertions.assertThat;
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
 * 验收标准（Task 11）：分类过滤正确；删帖为逻辑删除，列表不再出现（详情 404）。
 * 同时覆盖：未登录发帖 401、越权 403、最新/最热排序、置顶优先。
 * 测试用户以 s11_ 前缀创建并在用例结束后清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostApiIntegrationTest {

    private static final String TEST_USER_PREFIX = "s11_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void registerUsers() throws Exception {
        long suffix = System.nanoTime();
        tokenA = registerAndGetToken(TEST_USER_PREFIX + "a" + suffix);
        tokenB = registerAndGetToken(TEST_USER_PREFIX + "b" + suffix);
    }

    @AfterEach
    void cleanTestData() {
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
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
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    private long createPost(String token, long categoryId, String title) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId,
                                "title", title,
                                "contentMd", "# " + title + "\n\n正文内容"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.replyCount").value(0))
                .andExpect(jsonPath("$.data.viewCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private long categoryIdBySlug(String slug) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug = ?", Long.class, slug);
    }

    @Test
    void categoriesListFourSeeded() throws Exception {
        String resp = mockMvc.perform(get("/api/v1/posts/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(resp).path("data");
        assertThat(data.get(0).path("slug").asText()).isEqualTo("java");
        assertThat(data.get(3).path("slug").asText()).isEqualTo("resource");
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryIdBySlug("java"), "title", "t", "contentMd", "c"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void invalidCategoryReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", 999999, "title", "t", "contentMd", "c"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void categoryFilterWorks() throws Exception {
        long javaId = categoryIdBySlug("java");
        long aiId = categoryIdBySlug("ai");
        createPost(tokenA, javaId, "Java 分类帖子");
        createPost(tokenA, aiId, "AI 分类帖子");

        String listResp = mockMvc.perform(get("/api/v1/posts").param("category", "java"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode records = objectMapper.readTree(listResp).path("data").path("records");
        boolean nonJavaFound = false;
        for (JsonNode record : records) {
            if (record.path("categoryId").asLong() != javaId) {
                nonJavaFound = true;
            }
        }
        assertThat(nonJavaFound).isFalse();
        assertThat(records.size()).isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/v1/posts").param("category", "不存在分类"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void latestAndHotSorting() throws Exception {
        long javaId = categoryIdBySlug("java");
        long earlier = createPost(tokenA, javaId, "较早的帖子");
        long later = createPost(tokenA, javaId, "较新的帖子");

        // 最新：后创建的在前
        String latestResp = mockMvc.perform(get("/api/v1/posts")
                        .param("category", "java").param("sort", "latest"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(latestResp).path("data").path("records").get(0)
                .path("id").asLong()).isEqualTo(later);

        // 最热：给较早帖子加权（测试数据准备，直接调计数）
        jdbcTemplate.update("UPDATE post SET view_count = 100 WHERE id = ?", earlier);

        String hotResp = mockMvc.perform(get("/api/v1/posts")
                        .param("category", "java").param("sort", "hot"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(hotResp).path("data").path("records").get(0)
                .path("id").asLong()).isEqualTo(earlier);

        // 置顶恒在最前
        jdbcTemplate.update("UPDATE post SET is_pinned = 1 WHERE id = ?", earlier);
        String pinnedResp = mockMvc.perform(get("/api/v1/posts")
                        .param("category", "java").param("sort", "latest"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(pinnedResp).path("data").path("records").get(0)
                .path("id").asLong()).isEqualTo(earlier);
    }

    @Test
    void updateOnlyByOwner() throws Exception {
        long postId = createPost(tokenA, categoryIdBySlug("java"), "待编辑帖子");

        mockMvc.perform(put("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "越权编辑", "contentMd", "x"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(put("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "已编辑标题", "contentMd", "新内容"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("已编辑标题"));

        mockMvc.perform(put("/api/v1/posts/999999999")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "t", "contentMd", "c"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIsLogicalAndHiddenFromListAndDetail() throws Exception {
        long postId = createPost(tokenA, categoryIdBySlug("java"), "将被删除的帖子");

        // 他人删除 → 403
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 作者删除 → 200；物理行仍在（逻辑删除）
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        Integer deletedFlag = jdbcTemplate.queryForObject(
                "SELECT deleted FROM post WHERE id = ?", Integer.class, postId);
        assertThat(deletedFlag).isEqualTo(1);

        // 列表不再出现
        String listResp = mockMvc.perform(get("/api/v1/posts")
                        .param("category", "java"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(listResp).doesNotContain("将被删除的帖子");

        // 详情 404
        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }
}

package com.community.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
 * G1 搜索接口验收：命中/未命中/分页/HIDDEN 不泄露/通配符安全/空 q 与非法 type。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchIntegrationTest {

    private static final String PREFIX = "sg1_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private String marker;

    @BeforeEach
    void init() throws Exception {
        marker = "KW" + System.nanoTime();
        token = register(PREFIX + System.nanoTime());
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update(
                "DELETE FROM project_share WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void emptyOrMissingQReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("type", "post"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(get("/api/v1/search").param("q", "  ").param("type", "share"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void invalidTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "x").param("type", "all"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(get("/api/v1/search").param("q", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void anonymousCanSearchPostsHitAndMiss() throws Exception {
        createPost("标题含" + marker + "可搜", "正文普通");
        createPost("无关标题", "正文也无关键字");

        String hit = mockMvc.perform(get("/api/v1/search")
                        .param("q", marker).param("type", "post").param("size", "20"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(titles(hit)).anyMatch(t -> t.contains(marker));

        String miss = mockMvc.perform(get("/api/v1/search")
                        .param("q", "NOHIT" + marker).param("type", "post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(titles(miss)).isEmpty();
    }

    @Test
    void shareSearchRespectsPaginationAndHidden() throws Exception {
        long published = createShare("公开" + marker, List.of("Java"));
        long hidden = createShare("隐藏" + marker, List.of("Java"));
        mockMvc.perform(post("/api/v1/shares/" + hidden + "/hide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 造第二页：再插几条同关键字
        for (int i = 0; i < 3; i++) {
            createShare("分页" + marker + i, List.of("Vue"));
        }

        String page1 = mockMvc.perform(get("/api/v1/search")
                        .param("q", marker).param("type", "share")
                        .param("current", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(2))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(page1).path("data");
        assertThat(data.path("total").asLong()).isGreaterThanOrEqualTo(4);
        assertThat(data.path("records").size()).isEqualTo(2);

        // HIDDEN 标题不得出现
        String all = mockMvc.perform(get("/api/v1/search")
                        .param("q", marker).param("type", "share").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(titles(all)).noneMatch(t -> t.startsWith("隐藏"));
        assertThat(titles(all)).anyMatch(t -> t.contains("公开" + marker));
        assertThat(ids(all)).contains(published).doesNotContain(hidden);
    }

    @Test
    void likeWildcardPercentDoesNotMatchEverything() throws Exception {
        createShare("字面百分号" + marker + "100%off", List.of("AI"));
        createShare("普通" + marker + "无百分号", List.of("AI"));

        // 查询含 % 应只命中字面含 % 的标题，而非任意串
        String resp = mockMvc.perform(get("/api/v1/search")
                        .param("q", "100%off").param("type", "share").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> hitTitles = titles(resp);
        assertThat(hitTitles).anyMatch(t -> t.contains("100%off"));
        assertThat(hitTitles).noneMatch(t -> t.contains("无百分号"));
    }

    private List<String> titles(String json) throws Exception {
        JsonNode records = objectMapper.readTree(json).path("data").path("records");
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        for (JsonNode n : records) {
            list.add(n.path("title").asText());
        }
        return list;
    }

    private List<Long> ids(String json) throws Exception {
        JsonNode records = objectMapper.readTree(json).path("data").path("records");
        java.util.ArrayList<Long> list = new java.util.ArrayList<>();
        for (JsonNode n : records) {
            list.add(n.path("id").asLong());
        }
        return list;
    }

    private String register(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", username + "@test.local", "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    private long createPost(String title, String content) throws Exception {
        Long catId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug='java'", Long.class);
        String body = objectMapper.writeValueAsString(Map.of(
                "categoryId", catId, "title", title, "contentMd", content));
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private long createShare(String title, List<String> tags) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("summary", "摘要 " + title);
        payload.put("contentMd", "# " + title);
        payload.put("tags", tags);
        String resp = mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }
}

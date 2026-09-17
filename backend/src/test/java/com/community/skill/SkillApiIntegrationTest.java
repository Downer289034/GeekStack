package com.community.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
 * T-SKILL-1：发布/编辑/下架/越权 + HIDDEN 不泄露 + 点赞计数 + 搜索。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SkillApiIntegrationTest {

    private static final String PREFIX = "sk1_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        long s = System.nanoTime();
        tokenA = register(PREFIX + "a" + s);
        tokenB = register(PREFIX + "b" + s);
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update(
                "DELETE FROM notification WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM user_like WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM skill WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void createUpdateHideAndAuthz() throws Exception {
        String unique = "skilltag_" + System.nanoTime();
        long id = createSkill(tokenA, "Java 助手", unique);

        mockMvc.perform(get("/api/v1/skills/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java 助手"))
                .andExpect(jsonPath("$.data.tags[0]").value(unique));

        // 越权编辑
        mockMvc.perform(put("/api/v1/skills/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("越权", unique)))
                .andExpect(status().isForbidden());

        // 作者编辑
        mockMvc.perform(put("/api/v1/skills/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("Java 助手 v2", unique)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java 助手 v2"));

        // 下架后匿名 404；作者可见
        mockMvc.perform(post("/api/v1/skills/" + id + "/hide")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mockMvc.perform(get("/api/v1/skills/" + id))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/skills/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        // 广场不含 HIDDEN
        mockMvc.perform(get("/api/v1/skills").param("tag", unique))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    @Test
    void likeIncrementsCountAndSearchFindsPublished() throws Exception {
        String unique = "sksearch_" + System.nanoTime();
        long id = createSkill(tokenA, "异常分析助手 " + unique, unique);

        mockMvc.perform(post("/api/v1/likes/SKILL/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT like_count FROM skill WHERE id = ?", Integer.class, id);
        assertThat(likeCount).isEqualTo(1);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", unique)
                        .param("type", "skill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(id));

        // 下架后搜索不可见
        mockMvc.perform(post("/api/v1/skills/" + id + "/hide")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/search")
                        .param("q", unique)
                        .param("type", "skill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    @Test
    void createWithoutLogin401() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("t", "x")))
                .andExpect(status().isUnauthorized());
    }

    private long createSkill(String token, String title, String tag) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson(title, tag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private String payloadJson(String title, String tag) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "summary", "摘要 " + title,
                "contentMd", "# prompt\n\n" + title,
                "modelAdaptationMd", "GPT-4 / Claude",
                "tags", List.of(tag)));
    }

    private String register(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@test.local",
                "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }
}

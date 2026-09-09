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
 * G2 标签云：计数正确、HIDDEN 不计入、空库场景返回数组（本用例隔离唯一标签）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class TagCloudIntegrationTest {

    private static final String PREFIX = "stg2_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private String tagA;
    private String tagB;

    @BeforeEach
    void init() throws Exception {
        long sfx = System.nanoTime();
        token = register(PREFIX + sfx);
        tagA = "TagA" + sfx;
        tagB = "TagB" + sfx;
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update(
                "DELETE FROM project_share WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void countsPublishedOnlyAndIgnoresHidden() throws Exception {
        createShare("s1", List.of(tagA, tagB));
        createShare("s2", List.of(tagA));
        long hidden = createShare("s3-hidden", List.of(tagA, tagB));
        mockMvc.perform(post("/api/v1/shares/" + hidden + "/hide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String resp = mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Long> cloud = toMap(resp);
        assertThat(cloud.get(tagA)).isEqualTo(2L); // s1+s2，不含 hidden
        assertThat(cloud.get(tagB)).isEqualTo(1L); // 仅 s1
    }

    @Test
    void anonymousGetsEmptyArrayWhenNoMatchingTagsNeeded() throws Exception {
        // 不创建数据时接口仍应 200 且 data 为数组（可能含 demo 标签，结构正确即可）
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private Map<String, Long> toMap(String json) throws Exception {
        JsonNode arr = objectMapper.readTree(json).path("data");
        Map<String, Long> map = new HashMap<>();
        for (JsonNode n : arr) {
            map.put(n.path("name").asText(), n.path("count").asLong());
        }
        return map;
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

    private long createShare(String title, List<String> tags) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("summary", "tag cloud " + title);
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

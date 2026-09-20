package com.community.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * G2 / T-BE-4.2 标签云：share/skill/post 合并计数；HIDDEN/已删不计入。
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
    private String tagPostOnly;
    private long categoryId;

    @BeforeEach
    void init() throws Exception {
        long sfx = System.nanoTime();
        token = register(PREFIX + sfx);
        tagA = "TagA" + sfx;
        tagB = "TagB" + sfx;
        tagPostOnly = "PostOnly" + sfx;
        categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug = ?", Long.class, "java");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
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

    /** T-BE-4.2：仅有帖子标签时云仍出现该 tag。 */
    @Test
    void postOnlyTagAppearsInCloud() throws Exception {
        createPost("post-only-tag", List.of(tagPostOnly));

        String resp = mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Long> cloud = toMap(resp);
        assertThat(cloud.get(tagPostOnly)).isEqualTo(1L);
    }

    /** T-BE-4.2：同名 tag 在 post + share 间合并 count。 */
    @Test
    void postAndShareSameTagMergesCount() throws Exception {
        createPost("post-merge", List.of(tagA));
        createShare("share-merge", List.of(tagA));

        String resp = mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Long> cloud = toMap(resp);
        assertThat(cloud.get(tagA)).isEqualTo(2L);
    }

    /** T-BE-4.2：逻辑删除帖不计入云。 */
    @Test
    void deletedPostTagsExcludedFromCloud() throws Exception {
        long postId = createPost("post-to-delete", List.of(tagPostOnly));
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String resp = mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Long> cloud = toMap(resp);
        assertThat(cloud.containsKey(tagPostOnly)).isFalse();
    }

    @Test
    void anonymousGetsEmptyArrayWhenNoMatchingTagsNeeded() throws Exception {
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

    private long createPost(String title, List<String> tags) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId,
                                "title", title,
                                "contentMd", "body " + title,
                                "tags", tags))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }
}

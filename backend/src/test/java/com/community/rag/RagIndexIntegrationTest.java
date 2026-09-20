package com.community.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * T-AI-1：发布/编辑/删除三路径 + HIDDEN 不进库 + schema v1.1（mock embedding=无 Key 伪向量）。
 */
@SpringBootTest(properties = {
        "community.rag.qdrant-url=memory",
        "community.rag.dotenv-qdrant=false",
        "community.rag.dashscope-api-key="
})
@AutoConfigureMockMvc
class RagIndexIntegrationTest {

    private static final String PREFIX = "rag1_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RagVectorStore vectorStore;

    @Autowired
    private HelpCorpusSeedRunner helpCorpusSeedRunner;

    private String token;
    private String username;

    @BeforeEach
    void setUp() throws Exception {
        if (vectorStore instanceof InMemoryRagVectorStore mem) {
            mem.clear();
        }
        long s = System.nanoTime();
        username = PREFIX + "u" + s;
        token = register(username);
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM project_share WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM skill WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
        if (vectorStore instanceof InMemoryRagVectorStore mem) {
            mem.clear();
        }
    }

    @Test
    void postCreateUpdateDeleteThreePath() throws Exception {
        long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug = ?", Long.class, "java");
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId,
                                "title", "RAG index post",
                                "contentMd", "正文用于向量索引测试 " + System.nanoTime()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long postId = objectMapper.readTree(createResp).path("data").path("id").asLong();
        String parent = RagIndexService.parentContentId("post", postId);

        awaitCount(parent, 1);

        Map<String, Object> payload = vectorStore.findOnePayloadByParent(parent);
        assertSchemaV11(payload);
        assertThat(payload.get("parent_content_id")).isEqualTo(parent);
        assertThat(payload.get("content_type")).isEqualTo("post");
        assertThat(payload.get("embedding_model")).isEqualTo("text-embedding-v3");

        mockMvc.perform(put("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "RAG index post edited",
                                "contentMd", "编辑后的正文 " + System.nanoTime()))))
                .andExpect(status().isOk());

        awaitPayloadTitle(parent, "RAG index post edited");

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        awaitCount(parent, 0);
    }

    @Test
    void hiddenShareNotIndexedAndHideRemovesVectors() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "share for rag",
                                "summary", "summary",
                                "contentMd", "share body",
                                "repoUrl", "https://github.com/example/repo",
                                "tags", List.of("java")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long shareId = objectMapper.readTree(createResp).path("data").path("id").asLong();
        String parent = RagIndexService.parentContentId("share", shareId);
        awaitCount(parent, 1);

        mockMvc.perform(post("/api/v1/shares/" + shareId + "/hide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        awaitCount(parent, 0);
    }

    @Test
    void helpCorpusSeedWritesSchemaV11() {
        int n = helpCorpusSeedRunner.seedFromClasspath("rag/help-corpus.json");
        assertThat(n).isEqualTo(2);
        Map<String, Object> payload = vectorStore.findOnePayloadByParent("jkz_help_welcome");
        assertThat(payload).isNotEmpty();
        assertSchemaV11(payload);
        assertThat(payload.get("content_type")).isEqualTo("help");
    }

    private void assertSchemaV11(Map<String, Object> payload) {
        assertThat(payload).containsKeys(
                "content_id", "parent_content_id", "source_system", "content_type",
                "text_url", "author_id", "created_at", "visibility", "embedding_model",
                "chunk_index", "chunk_text", "text", "extra");
        assertThat(payload.get("text")).isEqualTo(payload.get("chunk_text"));
        assertThat(String.valueOf(payload.get("parent_content_id"))).startsWith("jkz_");
        assertThat(String.valueOf(payload.get("author_id"))).startsWith("u_");
        assertThat(payload.get("chunk_text")).isNotNull();
    }

    private void awaitCount(String parent, int expected) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            if (vectorStore.countByParentContentId(parent) == expected) {
                return;
            }
            Thread.sleep(250);
        }
        assertThat(vectorStore.countByParentContentId(parent)).isEqualTo(expected);
    }

    private void awaitPayloadTitle(String parent, String title) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            Map<String, Object> payload = vectorStore.findOnePayloadByParent(parent);
            if (title.equals(payload.get("title"))) {
                return;
            }
            Thread.sleep(250);
        }
        assertThat(vectorStore.findOnePayloadByParent(parent).get("title")).isEqualTo(title);
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

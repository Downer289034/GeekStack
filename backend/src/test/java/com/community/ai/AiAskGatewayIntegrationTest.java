package com.community.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-AI-3：未登录 401 / 超限 429 / 上游宕机 503。
 * 配额用 memory，上游指向未监听端口，不依赖 Redis 与真实 ai-service。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "community.ai.base-url=http://127.0.0.1:9",
        "community.ai.connect-timeout-ms=800",
        "community.ai.read-timeout-ms=1500",
        "community.ai.quota=memory",
        "community.ai.rate-limit-per-hour=2",
        "community.rag.dotenv-qdrant=false",
        "community.rag.qdrant-url=memory"
})
class AiAskGatewayIntegrationTest {

    private static final String PREFIX = "ai3_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String username = PREFIX + System.nanoTime();
        token = register(username);
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void anonymousAskUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("hello")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void upstreamDownReturns503() throws Exception {
        mockMvc.perform(post("/api/v1/ai/ask")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("极客栈怎么发帖？")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(50300))
                .andExpect(jsonPath("$.message").value("AI 助手暂时不可用，请稍后再试"));
    }

    @Test
    void thirdAskWithinHourReturns429() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/ai/ask")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("q" + i)))
                    .andExpect(status().isServiceUnavailable());
        }
        mockMvc.perform(post("/api/v1/ai/ask")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("q3")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(42900))
                .andExpect(jsonPath("$.message").value("提问过于频繁，请稍后再试"));
    }

    private String body(String question) throws Exception {
        return objectMapper.writeValueAsString(Map.of("question", question));
    }

    private String register(String username) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@test.local",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }
}

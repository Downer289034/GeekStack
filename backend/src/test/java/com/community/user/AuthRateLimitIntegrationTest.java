package com.community.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T-SEC-1-BE：登录频控集成——第 6 次失败 429、成功清零。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRateLimitIntegrationTest {

    private static final String PREFIX = "trl_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@test.local",
                                "password", "password123"))))
                .andExpect(status().isOk());
        redisTemplate.delete("auth:fail:127.0.0.1:" + username);
    }

    @Test
    void sixthLoginFailureReturns429() throws Exception {
        String username = PREFIX + "lock_" + System.nanoTime();
        register(username);

        String wrong = objectMapper.writeValueAsString(
                Map.of("username", username, "password", "wrong-password"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrong)
                            .with(req -> {
                                req.setRemoteAddr("127.0.0.1");
                                return req;
                            }))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrong)
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(42900));
    }

    @Test
    void successfulLoginClearsFailureCounter() throws Exception {
        String username = PREFIX + "clear_" + System.nanoTime();
        register(username);

        String wrong = objectMapper.writeValueAsString(
                Map.of("username", username, "password", "wrong-password"));
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrong)
                            .with(req -> {
                                req.setRemoteAddr("127.0.0.1");
                                return req;
                            }))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", "password123")))
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        }))
                .andExpect(status().isOk());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrong)
                            .with(req -> {
                                req.setRemoteAddr("127.0.0.1");
                                return req;
                            }))
                    .andExpect(status().isUnauthorized());
        }
    }
}

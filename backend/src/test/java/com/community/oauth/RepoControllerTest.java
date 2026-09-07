package com.community.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserRepoMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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

@SpringBootTest
@AutoConfigureMockMvc
class RepoControllerTest {

    private static final String TEST_USER_PREFIX = "t17r_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepoMapper repoMapper;

    private String token;
    private long userId;
    private String username;

    @BeforeEach
    void init() throws Exception {
        username = TEST_USER_PREFIX + System.nanoTime();
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "email", username + "@test.local", "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        token = objectMapper.readTree(resp).path("data").path("accessToken").asText();
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM user WHERE username = ?", Long.class, username);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM user_repo WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)", TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user_oauth_binding WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)", TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private long insertRepo(String fullName, int hidden) {
        UserRepo repo = new UserRepo();
        repo.setUserId(userId);
        repo.setProvider("GITHUB");
        repo.setRepoFullName(fullName);
        repo.setRepoUrl("https://github.com/" + fullName);
        repo.setStars(10L);
        repo.setHidden(hidden);
        repo.setSyncedAt(LocalDateTime.now());
        repoMapper.insert(repo);
        return repo.getId();
    }

    // ==================== GET /users/{username}/repos ====================

    @Test
    void listReposReturnsVisibleOnly() throws Exception {
        insertRepo("visible/repo", 0);
        insertRepo("hidden/repo", 1);

        mockMvc.perform(get("/api/v1/users/" + username + "/repos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].repoFullName").value("visible/repo"));
    }

    @Test
    void listReposReturnsEmptyForUnknownUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/nonexistent_xyz/repos"))
                .andExpect(status().isNotFound());
    }

    // ==================== POST /repos/{id}/toggle-hide ====================

    @Test
    void toggleHideFromVisibleToHidden() throws Exception {
        long repoId = insertRepo("test/repo", 0);

        mockMvc.perform(post("/api/v1/repos/" + repoId + "/toggle-hide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void toggleHideFromHiddenToVisible() throws Exception {
        long repoId = insertRepo("test/repo", 1);

        mockMvc.perform(post("/api/v1/repos/" + repoId + "/toggle-hide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void nonExistentRepoReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/repos/999999/toggle-hide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}

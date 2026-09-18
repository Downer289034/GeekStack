package com.community.skill;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * T-SKILL-3-BE：version/changelog 发布编辑返回 + SemVer 校验 + 三路径（本人/越权 403/未登录 401）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SkillVersionIntegrationTest {

    private static final String PREFIX = "sk3_";

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
                "DELETE FROM skill WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void ownerCreateAndUpdateVersionAppearsInDetailAndList() throws Exception {
        String tag = "sk3tag_" + System.nanoTime();
        String createBody = payload("SemVer Skill", tag, "1.2.0", "首发：支持异常栈解析");
        String createResp = mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("1.2.0"))
                .andExpect(jsonPath("$.data.changelog").value("首发：支持异常栈解析"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = objectMapper.readTree(createResp).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/skills/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("1.2.0"))
                .andExpect(jsonPath("$.data.changelog").value("首发：支持异常栈解析"));

        mockMvc.perform(get("/api/v1/skills").param("tag", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].version").value("1.2.0"))
                .andExpect(jsonPath("$.data.records[0].changelog").value("首发：支持异常栈解析"));

        mockMvc.perform(put("/api/v1/skills/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("SemVer Skill", tag, "1.3.0", "修复边界 case")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("1.3.0"))
                .andExpect(jsonPath("$.data.changelog").value("修复边界 case"));
    }

    @Test
    void otherUserUpdateForbidden() throws Exception {
        String tag = "sk3oth_" + System.nanoTime();
        long id = create(tokenA, tag, "1.0.0");

        mockMvc.perform(put("/api/v1/skills/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("hijack", tag, "9.9.9", "恶意")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void unauthenticatedCreateAndUpdate401() throws Exception {
        String tag = "sk3anon_" + System.nanoTime();
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("anon", tag, "1.0.0", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        long id = create(tokenA, tag, "1.0.0");
        mockMvc.perform(put("/api/v1/skills/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("anon-edit", tag, "1.0.1", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void invalidSemVerReturns400() throws Exception {
        String tag = "sk3bad_" + System.nanoTime();
        mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("bad", tag, "v1", "x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void missingVersionDefaultsTo100() throws Exception {
        String tag = "sk3def_" + System.nanoTime();
        Map<String, Object> body = new HashMap<>();
        body.put("title", "default ver");
        body.put("summary", "摘要");
        body.put("contentMd", "# p");
        body.put("tags", List.of(tag));
        mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("1.0.0"));
    }

    private long create(String token, String tag, String version) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/skills")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("t", tag, version, null)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private String payload(String title, String tag, String version, String changelog) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("summary", "摘要 " + title);
        map.put("contentMd", "# prompt\n" + title);
        map.put("modelAdaptationMd", "GPT-4");
        map.put("version", version);
        map.put("changelog", changelog);
        map.put("tags", List.of(tag));
        return objectMapper.writeValueAsString(map);
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

package com.community.share;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
 * 验收标准（Task 10）：列表分页正确（含 tags 筛选、仅 PUBLISHED）；详情页浏览数 +1；
 * 隐藏帖仅作者可见；编辑时省略可选字段即清空（P2-5）。
 * 测试用户以 s10_ 前缀创建并在用例结束后清理，分页断言用唯一标签隔离外部数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShareQueryIntegrationTest {

    private static final String TEST_USER_PREFIX = "s10_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String filterTag;

    @BeforeEach
    void initUserAndTag() throws Exception {
        String username = TEST_USER_PREFIX + System.nanoTime();
        tokenA = registerAndGetToken(username);
        filterTag = "t10filter" + System.nanoTime();
    }

    @AfterEach
    void cleanTestData() {
        jdbcTemplate.update(
                "DELETE FROM project_share WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
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

    private long createShare(String title, boolean withDemoUrl) throws Exception {
        Map<String, Object> payload = new HashMap<>(Map.of(
                "title", title,
                "summary", "T10 测试简介 " + title,
                "contentMd", "# " + title,
                "tags", java.util.List.of(filterTag)));
        if (withDemoUrl) {
            payload.put("demoUrl", "https://demo.example.com/" + title);
        }
        String resp = mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private void hideShare(long id) throws Exception {
        mockMvc.perform(post("/api/v1/shares/" + id + "/hide")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void pageAndTagFilterAndPagination() throws Exception {
        long first = createShare("T10第一篇", false);
        long second = createShare("T10第二篇", false);
        long third = createShare("T10第三篇", false);

        // 全量：唯一标签隔离，total == 3
        mockMvc.perform(get("/api/v1/shares").param("current", "1").param("size", "10").param("tag", filterTag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(3));

        // 第一页 size=2：新→旧应为 [第三篇, 第二篇]
        mockMvc.perform(get("/api/v1/shares").param("current", "1").param("size", "2").param("tag", filterTag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].id").value(third))
                .andExpect(jsonPath("$.data.records[1].id").value(second));

        // 第二页：剩 1 条，为最旧的第一篇
        mockMvc.perform(get("/api/v1/shares").param("current", "2").param("size", "2").param("tag", filterTag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(first))
                .andExpect(jsonPath("$.data.records[0].contentMd").doesNotExist());

        // 下架一篇后列表不再出现（仅 PUBLISHED）
        hideShare(first);
        mockMvc.perform(get("/api/v1/shares").param("current", "1").param("size", "10").param("tag", filterTag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        // 不存在的标签 → 空
        mockMvc.perform(get("/api/v1/shares").param("tag", "不存在的标签xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void detailIncrementsViewCount() throws Exception {
        long id = createShare("T10浏览数", false);

        mockMvc.perform(get("/api/v1/shares/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));
        mockMvc.perform(get("/api/v1/shares/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }

    @Test
    void hiddenShareVisibleOnlyToOwner() throws Exception {
        long id = createShare("T10隐藏帖", false);
        hideShare(id);

        // 匿名访问 → 404（对外不存在）
        mockMvc.perform(get("/api/v1/shares/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));

        // 作者本人可见
        mockMvc.perform(get("/api/v1/shares/" + id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
    }

    @Test
    void updateWithOmittedOptionalFieldClearsIt() throws Exception {
        long id = createShare("T10清空测试", true);

        // 编辑时不带 demoUrl → 应被清空而非保留旧值（P2-5）
        Map<String, Object> payload = Map.of(
                "title", "T10清空测试-更新",
                "summary", "更新后简介",
                "contentMd", "更新内容");
        mockMvc.perform(put("/api/v1/shares/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.demoUrl").doesNotExist());
    }
}

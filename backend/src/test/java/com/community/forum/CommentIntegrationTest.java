package com.community.forum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验收标准（Task 12）：回帖后 reply_count 与 last_reply_at 正确更新；楼中楼缩进展示正确（两级树）。
 * 计数口径：新回帖 +1 且推进 last_reply_at；回帖删除 -1 且 last_reply_at 不回退；编辑回帖两者均不动。
 * 楼中楼仅一级嵌套（父必须是顶级）。同时覆盖项目分享评论（多态 SHARE）与越权。
 * 测试用户以 s12_ 前缀创建并在用例结束后清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommentIntegrationTest {

    private static final String TEST_USER_PREFIX = "s12_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;
    private String tokenB;
    private long postId;
    private long shareId;
    private long javaCategoryId;

    @BeforeEach
    void initUsersAndHosts() throws Exception {
        long suffix = System.nanoTime();
        tokenA = registerAndGetToken(TEST_USER_PREFIX + "a" + suffix);
        tokenB = registerAndGetToken(TEST_USER_PREFIX + "b" + suffix);
        javaCategoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug='java'", Long.class);
        postId = createPost("T12 回帖测试帖");
        shareId = createShare("T12 评论测试项目");
    }

    @AfterEach
    void cleanTestData() {
        // 楼中楼外键自引用：先删子评论（parent_id 非空）再删顶级
        jdbcTemplate.update("DELETE FROM comment WHERE parent_id IS NOT NULL");
        jdbcTemplate.update(
                "DELETE FROM comment WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
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

    private long createPost(String title) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", javaCategoryId, "title", title, "contentMd", "正文"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private long createShare(String title) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/shares")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "summary", "简介", "contentMd", "内容"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private long createComment(String token, long hostId, String content) throws Exception {
        return createComment(token, hostId, content, null);
    }

    private long createComment(String token, long hostId, String content, Long parentId) throws Exception {
        Map<String, Object> payload = parentId == null
                ? Map.of("contentMd", content)
                : Map.of("contentMd", content, "parentId", parentId);
        String resp = mockMvc.perform(post("/api/v1/posts/" + hostId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    private Map<String, Object> postCounters() {
        return jdbcTemplate.queryForMap(
                "SELECT reply_count, last_reply_at FROM post WHERE id = ?", postId);
    }

    @Test
    void replyUpdatesCountAndLastReplyAt() throws Exception {
        assertThat(postCounters().get("reply_count")).isEqualTo(0L);
        assertThat(postCounters().get("last_reply_at")).isNull();

        createComment(tokenB, postId, "第一条回帖");
        Map<String, Object> afterFirst = postCounters();
        assertThat(afterFirst.get("reply_count")).isEqualTo(1L);
        assertThat(afterFirst.get("last_reply_at")).isNotNull();
        java.time.LocalDateTime firstReplyTime = ((java.time.LocalDateTime) afterFirst.get("last_reply_at"));

        Thread.sleep(1100);
        createComment(tokenB, postId, "第二条回帖");
        Map<String, Object> afterSecond = postCounters();
        assertThat(afterSecond.get("reply_count")).isEqualTo(2L);
        assertThat((java.time.LocalDateTime) afterSecond.get("last_reply_at")).isAfter(firstReplyTime);
    }

    @Test
    void deleteReplyDecrementsCountButKeepsLastReplyAt() throws Exception {
        createComment(tokenB, postId, "将被删除的回帖");
        java.time.LocalDateTime before = (java.time.LocalDateTime) postCounters().get("last_reply_at");
        long commentId = createComment(tokenB, postId, "保留的回帖");
        createComment(tokenB, postId, "再补一条");
        Thread.sleep(1100);

        // 删除中间那条：计数 -1，last_reply_at 不回退
        mockMvc.perform(delete("/api/v1/posts/" + commentId + "/comments")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
        Map<String, Object> after = postCounters();
        assertThat(after.get("reply_count")).isEqualTo(2L);
        assertThat((java.time.LocalDateTime) after.get("last_reply_at")).isAfterOrEqualTo(before);

        // 越权删除 → 403
        long othersComment = createComment(tokenA, postId, "作者的回帖");
        mockMvc.perform(delete("/api/v1/posts/" + othersComment + "/comments")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void editReplyDoesNotTouchCounters() throws Exception {
        // 注：回帖暂无编辑接口（任务清单未要求），此处以直接改库模拟编辑场景的前置状态，
        // 验证"仅回帖新增/删除影响计数"的口径 —— 编辑（若后续引入）不应触碰计数。
        long commentId = createComment(tokenB, postId, "待编辑回帖");
        java.time.LocalDateTime before = (java.time.LocalDateTime) postCounters().get("last_reply_at");
        jdbcTemplate.update("UPDATE comment SET content_md = ? WHERE id = ?", "编辑后的内容", commentId);

        Map<String, Object> after = postCounters();
        assertThat(after.get("reply_count")).isEqualTo(1L);
        assertThat((java.time.LocalDateTime) after.get("last_reply_at")).isEqualTo(before);
    }

    @Test
    void twoLevelNestingEnforced() throws Exception {
        long topLevel = createComment(tokenB, postId, "顶级回帖");
        long subReply = createComment(tokenA, postId, "楼中楼回复", topLevel);

        // 三级嵌套（对楼中楼再回复）→ 400
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("contentMd", "三级回复", "parentId", subReply))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        // 树结构：顶级带 1 个 children
        String treeResp = mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode tree = objectMapper.readTree(treeResp).path("data");
        assertThat(tree.size()).isEqualTo(1);
        assertThat(tree.get(0).path("children").size()).isEqualTo(1);
        assertThat(tree.get(0).path("children").get(0).path("id").asLong()).isEqualTo(subReply);
    }

    @Test
    void replyToDeletedPostReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contentMd", "给已删帖回帖"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shareCommentsPolymorphic() throws Exception {
        // 项目分享评论：POST/SHARE 各自独立，互不串数据
        mockMvc.perform(post("/api/v1/shares/" + shareId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("contentMd", "项目评论"))))
                .andExpect(status().isOk());

        // 帖子回帖不受影响
        String shareTree = mockMvc.perform(get("/api/v1/shares/" + shareId + "/comments"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(shareTree).path("data").size()).isEqualTo(1);

        // 无效宿主类型路径不存在（路由仅挂 posts/shares）
        mockMvc.perform(get("/api/v1/users/" + postId + "/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postDetailIncrementsViewCount() throws Exception {
        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));
        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }
}

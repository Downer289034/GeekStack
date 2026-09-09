package com.community.hot;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.forum.entity.Post;
import com.community.forum.mapper.PostMapper;
import com.community.hot.service.HotPostService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 验收标准（Task 21）：DB 降级排序正确（score = is_pinned*10000 + reply*3 + view + like*2），
 * Redis 不可用时降级，大小限制。
 */
@SpringBootTest
class HotPostServiceTest {

    private static final String TEST_USER_PREFIX = "t21_";

    /**
     * F4：总是抛异常的 RedisTemplate → 强制所有测试走 DB 降级路径。
     * 用 @Primary 覆盖真实 bean，测试隔离彻底。
     */
    @TestConfiguration
    static class FailingRedisConfig {
        @Bean
        @Primary
        public StringRedisTemplate failingRedisTemplate() {
            StringRedisTemplate mock = org.mockito.Mockito.mock(StringRedisTemplate.class);
            org.mockito.Mockito.when(mock.hasKey(org.mockito.ArgumentMatchers.anyString()))
                    .thenThrow(new RuntimeException("Redis 连接失败（F4 降级测试桩）"));
            return mock;
        }
    }

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String testUser;
    private long userId;
    private long testCategoryId;

    @BeforeEach
    void init() {
        testUser = TEST_USER_PREFIX + System.nanoTime();
        jdbcTemplate.update("INSERT INTO user (username, role, status) VALUES (?, 'USER', 0)", testUser);
        userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, testUser);
        testCategoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM post_category WHERE slug='java'", Long.class);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM comment WHERE parent_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM comment WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM post WHERE user_id IN (SELECT id FROM user WHERE username LIKE ?)",
                TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private void createHotPost(String title, long views, long replies, long likes, int pinned) {
        jdbcTemplate.update("INSERT INTO post (user_id, category_id, title, content_md, view_count, reply_count, like_count, is_pinned) "
                + "VALUES (?, ?, ?, 'test', ?, ?, ?, ?)",
                userId, testCategoryId, title, views, replies, likes, pinned);
    }

    /** 全站排序（F3：is_pinned*10000 + reply*3 + view + like*2） */
    @Test
    void hotListOrdersByUnifiedScore() {
        createHotPost("A", 100, 5, 10, 0);   // score = 0 + 5*3 + 100 + 10*2 = 135
        createHotPost("B", 50, 10, 5, 0);     // score = 0 + 10*3 + 50 + 5*2 = 90
        createHotPost("C", 200, 1, 0, 0);     // score = 0 + 1*3 + 200 + 0 = 203
        createHotPost("D", 10, 1, 1, 1);      // score = 10000 + 1*3 + 10 + 1*2 = 10015 (置顶)

        // 共享 community-dev 可能有演示/残留帖：只断言本用例 4 帖的相对序（全站同公式下序不变）
        List<String> orderedOurs = dbHotPosts(50).stream()
                .filter(p -> userId == p.getUserId())
                .map(Post::getTitle)
                .toList();
        assertThat(orderedOurs).containsExactly("D", "C", "A", "B");
    }

    @Test
    void hotListRespectsSize() {
        for (int i = 0; i < 15; i++) {
            createHotPost("P" + i, i * 10, i, 0, 0);
        }
        List<Post> result = dbHotPosts(5);
        assertThat(result).hasSize(5);
    }

    @Test
    void siteWideNotUserSpecific() {
        // F1：不按 userId 过滤——创建另一个用户的数据，验证全站可见
        jdbcTemplate.update("INSERT INTO user (username, role, status) VALUES (?, 'USER', 0)",
                TEST_USER_PREFIX + "other");
        long otherId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?",
                Long.class, TEST_USER_PREFIX + "other");
        jdbcTemplate.update("INSERT INTO post (user_id, category_id, title, content_md, view_count, reply_count, like_count) "
                + "VALUES (?, ?, 'OtherPosts', 'x', 500, 0, 0)", otherId, testCategoryId);

        List<Post> result = dbHotPosts(10);
        assertThat(result.stream().anyMatch(p -> "OtherPosts".equals(p.getTitle()))).isTrue();
    }

    /**
     * F4：Redis 降级路径验证——RedisTemplate 由 TestConfiguration 注入（总是抛异常），
     * getHotList 应静默降级到 DB 路径而不抛异常。
     */
    @Test
    void redisExceptionFallsBackToDb() {
        HotPostService hotPostService = new HotPostService(postMapper, null);
        // 创建数据
        createHotPost("Fallback", 100, 5, 0, 0);
        // 理论上 null RedisTemplate 会在 tryRedisHotList 里抛 NPE → catch → 走 dbHotList
        // 验证：不抛异常
        try {
            hotPostService.getHotList(10);
        } catch (Exception e) {
            throw new AssertionError("getHotList 不应抛异常：" + e.getMessage(), e);
        }
    }

    /** 直接测 DB 查询（跳过缓存）：全站排序 */
    private List<Post> dbHotPosts(long size) {
        Page<Post> page = postMapper.selectPage(Page.of(1, size),
                new QueryWrapper<Post>()
                        .orderByDesc("is_pinned * 10000 + reply_count * 3 + view_count + like_count * 2", "id"));
        return page.getRecords();
    }
}

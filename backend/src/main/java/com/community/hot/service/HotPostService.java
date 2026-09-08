package com.community.hot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.PageResponse;
import com.community.forum.dto.PostCardResponse;
import com.community.forum.entity.Post;
import com.community.forum.mapper.PostMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 热帖榜单服务（T21）：
 * - Redis ZSet 为主路径；Redis 不可用时降级到 DB（try-catch 隔离）
 * - score = is_pinned*10000 + reply_count*3 + view_count + like_count*2（两路统一公式）
 * - 定时任务（@Scheduled）每 30 分钟刷新 ZSet（24h 窗口，最多 500 条）
 * - 60s 本地缓存（key 含 size 维度，避免固定大小）
 */
@Service
public class HotPostService {

    private static final Logger log = LoggerFactory.getLogger(HotPostService.class);
    private static final String ZSET_KEY = "hot:posts";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final PostMapper postMapper;
    private final StringRedisTemplate redisTemplate;

    /** 60s 内存缓存：size → (result, expiryMs) */
    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    public HotPostService(PostMapper postMapper, StringRedisTemplate redisTemplate) {
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取热帖列表（60s 缓存 → Redis ZSet → DB 降级）。
     * 全站热帖（F1：不限定 userId）。
     */
    public List<PostCardResponse> getHotList(long size) {
        // 1. 60s 本地缓存（含 size 维度）
        CacheEntry entry = cache.get(size);
        if (entry != null && System.currentTimeMillis() < entry.expiryMs()) {
            return entry.data();
        }

        // 2. 尝试 Redis ZSet（降级返回 null）
        List<PostCardResponse> result = tryRedisHotList(size);

        // 3. Redis 不可用 → DB 降级
        if (result == null) {
            result = dbHotList(size);
        }

        // 4. 更新缓存
        cache.put(size, new CacheEntry(result, System.currentTimeMillis() + CACHE_TTL.toMillis()));
        return result;
    }

    /** Redis 降级封装：返回 null 表示降级。 */
    private List<PostCardResponse> tryRedisHotList(long size) {
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(ZSET_KEY))) {
                return null;
            }
            Set<String> topIds = redisTemplate.opsForZSet().reverseRange(ZSET_KEY, 0, size - 1);
            if (topIds == null || topIds.isEmpty()) {
                return null;
            }
            List<Long> ids = topIds.stream().map(Long::parseLong).toList();
            List<Post> posts = postMapper.selectBatchIds(ids);
            return ids.stream()
                    .map(id -> posts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .map(this::toCard)
                    .toList();
        } catch (Exception e) {
            log.warn("Redis ZSet 查询失败，降级到 DB: {}", e.getMessage());
            return null;
        }
    }

    /**
     * DB 降级路径：全站热帖（F1：不限 userId）。
     * score 公式与 ZSet 统一：is_pinned*10000 + reply_count*3 + view_count + like_count*2（F3）。
     */
    private List<PostCardResponse> dbHotList(long size) {
        Page<Post> page = postMapper.selectPage(Page.of(1, size),
                new QueryWrapper<Post>()
                        .orderByDesc(
                                // F3：统一口径 = is_pinned*10000 + reply*3 + view + like*2
                                "is_pinned * 10000 + reply_count * 3 + view_count + like_count * 2",
                                "id"));
        return page.getRecords().stream().map(this::toCard).toList();
    }

    /**
     * 定时任务：每 30 分钟刷新 ZSet（24h 窗口帖子）。
     * score = is_pinned*10000 + reply_count*3 + view_count + like_count*2
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void refreshZSet() {
        try {
            redisTemplate.delete(ZSET_KEY);
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<Post> recentPosts = postMapper.selectList(
                    Wrappers.<Post>lambdaQuery()
                            .ge(Post::getCreatedAt, since)
                            .orderByDesc(Post::getCreatedAt)
                            .last("LIMIT 500"));
            for (Post p : recentPosts) {
                double score = computeScore(p);
                redisTemplate.opsForZSet().add(ZSET_KEY, String.valueOf(p.getId()), score);
            }
            // 清空 60s 缓存
            cache.clear();
            log.info("ZSet 刷新完成: size={}", recentPosts.size());
        } catch (Exception e) {
            log.warn("ZSet 刷新失败(降级到 DB 排序): {}", e.getMessage());
        }
    }

    /**
     * 统一热帖评分公式（两路一致）：
     * score = is_pinned*10000 + reply_count*3 + view_count + like_count*2
     * 置顶优先：is_pinned=1 的帖子永远排在前面（10000 足够大）。
     */
    private double computeScore(Post p) {
        long pinned = (p.getIsPinned() != null && p.getIsPinned() == 1) ? 10000L : 0;
        long reply = p.getReplyCount() != null ? p.getReplyCount() : 0;
        long view = p.getViewCount() != null ? p.getViewCount() : 0;
        long like = p.getLikeCount() != null ? p.getLikeCount() : 0;
        return pinned + reply * 3 + view + like * 2;
    }

    private PostCardResponse toCard(Post post) {
        return new PostCardResponse(post.getId(), post.getUserId(), post.getCategoryId(),
                post.getTitle(), post.getViewCount(), post.getReplyCount(),
                post.getLastReplyAt(), post.getIsPinned(), post.getIsHot(), post.getCreatedAt());
    }

    private record CacheEntry(List<PostCardResponse> data, long expiryMs) {}
}

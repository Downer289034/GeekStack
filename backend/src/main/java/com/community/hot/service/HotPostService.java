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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 热帖榜单服务（T21）：
 * - Redis ZSet 为主路径，score = reply_count*3 + view_count + like_count*2（写在注释）
 * - 定时任务（@Scheduled cron）刷新 ZSet，24h 窗口
 * - Redis 不可用时降级回 DB 排序（try-catch 返回现有 hot 排序）
 * - 首页热榜接口加 60s 本地缓存（手动 ConcurrentHashMap）
 */
@Service
public class HotPostService {

    private static final Logger log = LoggerFactory.getLogger(HotPostService.class);
    private static final String ZSET_KEY = "hot:posts";
    private static final String CACHE_KEY = "hot_list";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final PostMapper postMapper;
    private final StringRedisTemplate redisTemplate;

    /** 60s 内存缓存：key → 缓存值+过期时间戳 */
    private volatile List<PostCardResponse> cachedHotList;
    private volatile long cacheExpiryMs = 0;

    public HotPostService(PostMapper postMapper, StringRedisTemplate redisTemplate) {
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取热帖列表（60s 缓存 → Redis ZSet → DB 降级）。
     * score = reply_count*3 + view_count + like_count*2
     */
    public List<PostCardResponse> getHotList(long size, long userId) {
        // 1. 检查 60s 本地缓存
        if (cachedHotList != null && System.currentTimeMillis() < cacheExpiryMs) {
            return cachedHotList.stream().limit(size).collect(Collectors.toList());
        }

        List<PostCardResponse> result = tryRedisHotList(size);

        // 2. Redis 降级：Redis 不可用时回 DB
        if (result == null) {
            result = dbHotList(size, userId);
        }

        // 3. 更新缓存
        cachedHotList = result;
        cacheExpiryMs = System.currentTimeMillis() + CACHE_TTL.toMillis();
        return result;
    }

    /**
     * 从 Redis ZSet 获取热帖（score = reply*3 + view + like*2）。
     * Redis 不可用时返回 null（降级路径）。
     */
    private List<PostCardResponse> tryRedisHotList(long size) {
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(ZSET_KEY))) {
                return null;
            }
            Set<String> topIds = redisTemplate.opsForZSet()
                    .reverseRange(ZSET_KEY, 0, size - 1);
            if (topIds == null || topIds.isEmpty()) {
                return null;
            }
            // 批量查库获取帖子详情，按 Redis ZSet 排序
            List<Long> ids = topIds.stream().map(Long::parseLong).toList();
            List<Post> posts = postMapper.selectBatchIds(ids);
            Map<Long, Post> postMap = posts.stream()
                    .collect(Collectors.toMap(Post::getId, p -> p));
            return ids.stream()
                    .map(postMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(this::toCard)
                    .toList();
        } catch (Exception e) {
            log.warn("Redis ZSet 查询失败，降级到 DB: {}", e.getMessage());
            return null;
        }
    }

    /** DB 降级路径：按 hot 排序（复用 T11 的既有口径），限定当前用户帖子。 */
    private List<PostCardResponse> dbHotList(long size, long userId) {
        Page<Post> page = postMapper.selectPage(Page.of(1, size),
                new QueryWrapper<Post>()
                        .eq("user_id", userId)
                        .orderByDesc("is_pinned", "reply_count * 3 + view_count", "id"));
        return page.getRecords().stream().map(this::toCard).toList();
    }

    /**
     * 定时任务：每 30 分钟刷新 ZSet（24h 窗口帖子）。
     * score = reply_count*3 + view_count + like_count*2
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void refreshZSet() {
        try {
            // 清空旧 ZSet（全量替换，避免过期帖子残留）
            redisTemplate.delete(ZSET_KEY);

            // 查最近 24h 的帖子
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<Post> recentPosts = postMapper.selectList(
                    Wrappers.<Post>lambdaQuery()
                            .ge(Post::getCreatedAt, since)
                            .orderByDesc(Post::getCreatedAt)
                            .last("LIMIT 500"));
            if (recentPosts.isEmpty()) {
                return;
            }

            // 批量写入 ZSet：score = reply*3 + view + like*2
            for (Post p : recentPosts) {
                long reply = p.getReplyCount() != null ? p.getReplyCount() : 0;
                long view = p.getViewCount() != null ? p.getViewCount() : 0;
                long like = p.getLikeCount() != null ? p.getLikeCount() : 0;
                double score = reply * 3 + view + like * 2;
                redisTemplate.opsForZSet().add(ZSET_KEY, String.valueOf(p.getId()), score);
            }

            // 清除 60s 缓存以让下次请求拿到新数据
            cachedHotList = null;
            cacheExpiryMs = 0;

            log.info("ZSet 刷新完成: size={}", recentPosts.size());
        } catch (Exception e) {
            log.warn("ZSet 刷新失败(降级到 DB 排序): {}", e.getMessage());
        }
    }

    private PostCardResponse toCard(Post post) {
        return new PostCardResponse(post.getId(), post.getUserId(), post.getCategoryId(),
                post.getTitle(), post.getViewCount(), post.getReplyCount(),
                post.getLastReplyAt(), post.getIsPinned(), post.getIsHot(), post.getCreatedAt());
    }
}

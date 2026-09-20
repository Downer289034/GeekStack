package com.community.ai;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 用户级提问配额：默认每小时 10 次。
 * Redis 不可用时放行并 WARN（与认证频控同一降级口径，避免助手整体不可用）。
 */
@Component
public class AiAskQuota {

    private static final Logger log = LoggerFactory.getLogger(AiAskQuota.class);
    private static final String KEY_PREFIX = "ai:ask:";
    private static final Duration WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final AiProperties properties;
    private final ConcurrentHashMap<String, Window> memory = new ConcurrentHashMap<>();

    public AiAskQuota(StringRedisTemplate redisTemplate, AiProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void consume(String username) {
        int limit = Math.max(1, properties.getRateLimitPerHour());
        if (properties.memoryQuota()) {
            consumeMemory(username, limit);
            return;
        }
        String key = KEY_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
            if (count != null && count > limit) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "提问过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 限流 Redis 不可用，放行: user={}, error={}", username, e.getMessage());
        }
    }

    private void consumeMemory(String username, int limit) {
        long now = System.currentTimeMillis();
        memory.compute(username, (k, window) -> {
            if (window == null || now - window.startMs >= WINDOW.toMillis()) {
                Window fresh = new Window();
                fresh.startMs = now;
                fresh.count = 1;
                return fresh;
            }
            if (window.count >= limit) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "提问过于频繁，请稍后再试");
            }
            window.count++;
            return window;
        });
    }

    private static final class Window {
        private long startMs;
        private int count;
    }
}

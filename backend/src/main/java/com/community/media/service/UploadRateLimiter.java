package com.community.media.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 上传频率限制：每用户每分钟 N 次（Redis INCR；Redis 不可用时放行并打 WARN）。
 */
@Component
public class UploadRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(UploadRateLimiter.class);
    private static final String KEY_PREFIX = "media:upload:rate:";

    private final StringRedisTemplate redisTemplate;
    private final int limitPerMinute;

    /**
     * 构造。
     *
     * @param redisTemplate   Redis
     * @param limitPerMinute  每分钟上限
     */
    public UploadRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${community.media.rate-limit-per-minute:20}") int limitPerMinute) {
        this.redisTemplate = redisTemplate;
        this.limitPerMinute = limitPerMinute;
    }

    /**
     * 检查并计数；超限抛 {@link ErrorCode#TOO_MANY_REQUESTS}。
     *
     * @param username 登录用户名
     */
    public void check(String username) {
        String key = KEY_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            if (count != null && count > limitPerMinute) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "上传过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("上传限流 Redis 不可用，放行: user={}, error={}", username, e.getMessage());
        }
    }
}

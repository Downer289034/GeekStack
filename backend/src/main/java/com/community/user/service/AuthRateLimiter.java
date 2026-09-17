package com.community.user.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 认证接口失败频控：IP+用户名，15 分钟内失败 5 次后锁定（第 6 次起 429）。
 * Redis 不可用时放行并打 WARN（降级，避免拖垮登录）。
 */
@Component
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);
    static final String KEY_PREFIX = "auth:fail:";
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public AuthRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 已达上限则 429；Redis 异常放行。 */
    public void checkAllowed(String ip, String username) {
        String key = key(ip, username);
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw != null) {
                int count = Integer.parseInt(raw);
                if (count >= MAX_FAILURES) {
                    throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录尝试过多，请稍后再试");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("认证频控 Redis 不可用，放行: key={}, error={}", key, e.getMessage());
        }
    }

    /** 记录一次失败；Redis 异常静默。 */
    public void recordFailure(String ip, String username) {
        String key = key(ip, username);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
        } catch (Exception e) {
            log.warn("认证频控计数失败，忽略: key={}, error={}", key, e.getMessage());
        }
    }

    /** 成功登录/注册后清零。 */
    public void clear(String ip, String username) {
        String key = key(ip, username);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("认证频控清零失败，忽略: key={}, error={}", key, e.getMessage());
        }
    }

    static String key(String ip, String username) {
        String safeIp = ip == null || ip.isBlank() ? "unknown" : ip.trim();
        String safeUser = username == null || username.isBlank() ? "unknown" : username.trim();
        return KEY_PREFIX + safeIp + ":" + safeUser;
    }
}

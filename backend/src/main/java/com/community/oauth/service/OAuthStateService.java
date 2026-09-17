package com.community.oauth.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * OAuth CSRF state：SecureRandom ≥128bit，Redis TTL 5 分钟，校验即删除（防重放）。
 * 可由后端签发，也可接受前端传入的合规 state（便于 FE 并行 sessionStorage 流程）。
 */
@Service
public class OAuthStateService {

    private static final Logger log = LoggerFactory.getLogger(OAuthStateService.class);
    private static final String KEY_PREFIX = "oauth:state:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int STATE_BYTES = 16;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthStateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Redis key（测试过期场景可直接 delete）。 */
    public static String redisKey(String state) {
        return KEY_PREFIX + state;
    }

    /**
     * 签发 state 并写入 Redis；value=provider。
     *
     * @param provider GITHUB|GITEE
     * @return hex state（32 字符）
     */
    public String issue(String provider) {
        return issue(provider, null);
    }

    /**
     * 签发或接纳前端传入的 state。clientState 合规（≥32 hex）则复用，否则后端生成。
     */
    public String issue(String provider, String clientState) {
        String state = isValidClientState(clientState) ? clientState.trim() : generateState();
        try {
            redisTemplate.opsForValue().set(redisKey(state), provider, TTL);
        } catch (Exception e) {
            log.error("OAuth state 写入 Redis 失败: provider={}, error={}", provider, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录暂不可用，请稍后重试");
        }
        return state;
    }

    /**
     * 校验并消费 state；缺失/不匹配/过期/重放一律 401。
     */
    public void consumeOrThrow(String state, String provider) {
        if (state == null || state.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 OAuth state");
        }
        String key = redisKey(state.trim());
        String stored;
        try {
            stored = redisTemplate.opsForValue().getAndDelete(key);
        } catch (Exception e) {
            log.error("OAuth state 校验 Redis 失败: error={}", e.getMessage());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 OAuth state");
        }
        if (stored == null || !stored.equalsIgnoreCase(provider)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 OAuth state");
        }
    }

    private String generateState() {
        byte[] buf = new byte[STATE_BYTES];
        secureRandom.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static boolean isValidClientState(String clientState) {
        if (clientState == null) {
            return false;
        }
        String s = clientState.trim();
        // ≥128 bit：至少 32 个 hex 字符
        return s.length() >= 32 && s.length() <= 128 && s.chars().allMatch(c ->
                (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
    }
}

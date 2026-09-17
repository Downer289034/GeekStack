package com.community.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.user.service.AuthRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 钉死 Redis 宕机降级：频控检查/计数/清零均放行且不抛业务异常。
 */
class AuthRateLimiterDegradeTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private AuthRateLimiter limiter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenThrow(new RuntimeException("Redis down"));
        when(ops.increment(anyString())).thenThrow(new RuntimeException("Redis down"));
        doThrow(new RuntimeException("Redis down")).when(redis).delete(anyString());
        limiter = new AuthRateLimiter(redis);
    }

    @Test
    void checkAllowedDegradesToAllow() {
        assertThatCode(() -> limiter.checkAllowed("1.2.3.4", "alice")).doesNotThrowAnyException();
    }

    @Test
    void recordFailureDegradesSilently() {
        assertThatCode(() -> limiter.recordFailure("1.2.3.4", "alice")).doesNotThrowAnyException();
    }

    @Test
    void clearDegradesSilently() {
        assertThatCode(() -> limiter.clear("1.2.3.4", "alice")).doesNotThrowAnyException();
    }

    @Test
    void lockedWhenCountAtLeastFive() {
        StringRedisTemplate okRedis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> okOps = mock(ValueOperations.class);
        when(okRedis.opsForValue()).thenReturn(okOps);
        when(okOps.get("auth:fail:1.1.1.1:bob")).thenReturn("5");
        AuthRateLimiter okLimiter = new AuthRateLimiter(okRedis);

        assertThatThrownBy(() -> okLimiter.checkAllowed("1.1.1.1", "bob"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(
                        ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
    }
}

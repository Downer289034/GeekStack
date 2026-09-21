package com.community.oauth.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 手动仓库同步防抖：同用户同 provider 冷却期内拒绝（进程内内存；多实例需另接 Redis）。
 */
@Component
public class RepoSyncRateLimiter {

    private final ConcurrentHashMap<String, Long> lastAtMs = new ConcurrentHashMap<>();
    private final long cooldownMs;

    public RepoSyncRateLimiter(
            @Value("${community.oauth.repo-sync-cooldown-seconds:60}") int cooldownSeconds) {
        this.cooldownMs = Math.max(1, cooldownSeconds) * 1000L;
    }

    /** 冷却秒数（契约/测例可读）。 */
    public long cooldownSeconds() {
        return cooldownMs / 1000L;
    }

    /**
     * 通过则打点；冷却期内抛 {@link ErrorCode#TOO_MANY_REQUESTS}。
     */
    public void checkAndMark(long userId, String provider) {
        String key = userId + ":" + provider;
        long now = System.currentTimeMillis();
        lastAtMs.compute(key, (k, last) -> {
            if (last != null && now - last < cooldownMs) {
                long remainSec = Math.max(1L, (cooldownMs - (now - last) + 999L) / 1000L);
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                        "同步过于频繁，请 " + remainSec + " 秒后再试");
            }
            return now;
        });
    }

    /** 测例用：清空全部打点。 */
    public void clear() {
        lastAtMs.clear();
    }
}

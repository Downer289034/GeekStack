package com.community.oauth.dto;

/**
 * 手动同步仓库请求体（与 query {@code provider} 二选一，query 优先）。
 */
public record ManualRepoSyncRequest(String provider) {
}

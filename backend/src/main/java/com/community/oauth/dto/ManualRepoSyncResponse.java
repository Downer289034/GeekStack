package com.community.oauth.dto;

/**
 * 手动同步仓库成功响应。
 *
 * @param provider    GITHUB / GITEE
 * @param syncedCount 本次写入快照条数
 */
public record ManualRepoSyncResponse(String provider, int syncedCount) {
}

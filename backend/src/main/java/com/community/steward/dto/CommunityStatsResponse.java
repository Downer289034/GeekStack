package com.community.steward.dto;

import java.time.Instant;

/**
 * 社区聚合统计（只读）。{@code penaltyCount} 一期恒为 0（回流占位）。
 */
public record CommunityStatsResponse(
        long userCount,
        long bannedUserCount,
        long postCount,
        long commentCount,
        long shareCount,
        long skillCount,
        long penaltyCount,
        String penaltyNote,
        Instant generatedAt
) {
}

package com.community.share.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目分享响应（作者资料在主页/详情聚合时再扩展）。
 */
public record ShareResponse(
        Long id,
        Long userId,
        String title,
        String summary,
        String contentMd,
        String repoUrl,
        String demoUrl,
        List<String> tags,
        String status,
        Long viewCount,
        LocalDateTime createdAt
) {
}

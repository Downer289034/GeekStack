package com.community.search.dto;

/**
 * 标签云条目（仅统计 PUBLISHED 项目分享）。
 */
public record TagCountResponse(String name, long count) {
}

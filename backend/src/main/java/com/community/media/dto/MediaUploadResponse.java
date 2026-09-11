package com.community.media.dto;

/**
 * 上传成功响应：对外 URL + 对象键 + 元数据。
 */
public record MediaUploadResponse(
        String url,
        String key,
        String contentType,
        long size
) {
}

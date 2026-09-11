package com.community.media.service;

import java.io.InputStream;

/**
 * 对象存储抽象（M7-2）：实现可换 MinIO / 云 OSS，调用方无感。
 */
public interface StorageService {

    /**
     * 写入对象。
     *
     * @param key         对象键（已由上层生成，不含用户原始文件名）
     * @param data        内容流
     * @param size        字节数
     * @param contentType MIME
     */
    void put(String key, InputStream data, long size, String contentType);

    /**
     * 删除对象（不存在时静默成功）。
     *
     * @param key 对象键
     */
    void delete(String key);

    /**
     * 拼对外可读 URL（Nginx /media/ 前缀）。
     *
     * @param key 对象键，形如 media/2026/09/uuid.webp
     * @return 完整 URL
     */
    String publicUrl(String key);
}

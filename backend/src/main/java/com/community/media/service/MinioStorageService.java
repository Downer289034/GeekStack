package com.community.media.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * MinIO 实现的 {@link StorageService}。
 */
@Service
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;
    private final String publicBaseUrl;

    /**
     * 构造。
     *
     * @param minioClient    客户端
     * @param bucket         桶名
     * @param publicBaseUrl  对外 URL 前缀（无尾斜杠）
     */
    public MinioStorageService(
            MinioClient minioClient,
            @Value("${community.media.bucket}") String bucket,
            @Value("${community.media.public-base-url}") String publicBaseUrl) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
    }

    @Override
    public void put(String key, InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("MinIO put 失败: key={}, error={}", key, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件存储失败");
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO delete 失败(忽略): key={}, error={}", key, e.getMessage());
        }
    }

    @Override
    public String publicUrl(String key) {
        // key = media/yyyy/MM/uuid.ext → /media/yyyy/MM/uuid.ext
        String path = key.startsWith("media/") ? key.substring("media/".length()) : key;
        return publicBaseUrl + "/media/" + path;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

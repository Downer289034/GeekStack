package com.community.media.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端与媒体配置属性。
 */
@Configuration
public class MediaStorageConfig {

    /**
     * MinIO / S3 兼容客户端。
     *
     * @param endpoint  API 端点
     * @param accessKey 访问键
     * @param secretKey 密钥
     * @return 客户端
     */
    @Bean
    public MinioClient minioClient(
            @Value("${community.media.endpoint}") String endpoint,
            @Value("${community.media.access-key}") String accessKey,
            @Value("${community.media.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}

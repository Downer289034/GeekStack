package com.community.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 网关配置。上游契约见 docs/T-AI-2-问答链路交付.md §5。
 */
@ConfigurationProperties(prefix = "community.ai")
public class AiProperties {

    private String baseUrl = "http://127.0.0.1:8090";

    private int connectTimeoutMs = 2000;

    private int readTimeoutMs = 120_000;

    private int rateLimitPerHour = 10;

    /** redis 或 memory。 */
    private String quota = "redis";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getRateLimitPerHour() {
        return rateLimitPerHour;
    }

    public void setRateLimitPerHour(int rateLimitPerHour) {
        this.rateLimitPerHour = rateLimitPerHour;
    }

    public String getQuota() {
        return quota;
    }

    public void setQuota(String quota) {
        this.quota = quota;
    }

    public boolean memoryQuota() {
        return "memory".equalsIgnoreCase(quota);
    }
}

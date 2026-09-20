package com.community.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 索引配置（T-AI-1 定稿：512/50、text-embedding-v3、schema v1.1）。
 */
@ConfigurationProperties(prefix = "community.rag")
public class RagProperties {

    /** 总开关；false 时监听器空操作。 */
    private boolean enabled = true;

    private String sourceSystem = "geekstack";

    private int chunkSizeTokens = 512;

    private int chunkOverlapTokens = 50;

    private String embeddingModel = "text-embedding-v3";

    private int embeddingDim = 1024;

    private String dashscopeApiKey = "";

    private String dashscopeBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 空或 memory → 进程内向量库（测试/无 Qdrant）；否则 Qdrant HTTP。 */
    private String qdrantUrl = "memory";

    private String qdrantCollection = "geekstack_community";

    /** 拼 text_url 用，不含尾斜杠。 */
    private String publicBaseUrl = "http://localhost:5173";

    private int connectTimeoutMs = 2000;

    private int readTimeoutMs = 5000;

    private int maxRetries = 2;

    private int embeddingBatchSize = 10;

    /** 启动时灌入平台帮助语料（一次性）。 */
    private boolean seedHelpOnStartup = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public int getChunkSizeTokens() {
        return chunkSizeTokens;
    }

    public void setChunkSizeTokens(int chunkSizeTokens) {
        this.chunkSizeTokens = chunkSizeTokens;
    }

    public int getChunkOverlapTokens() {
        return chunkOverlapTokens;
    }

    public void setChunkOverlapTokens(int chunkOverlapTokens) {
        this.chunkOverlapTokens = chunkOverlapTokens;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getEmbeddingDim() {
        return embeddingDim;
    }

    public void setEmbeddingDim(int embeddingDim) {
        this.embeddingDim = embeddingDim;
    }

    public String getDashscopeApiKey() {
        return dashscopeApiKey;
    }

    public void setDashscopeApiKey(String dashscopeApiKey) {
        this.dashscopeApiKey = dashscopeApiKey;
    }

    public String getDashscopeBaseUrl() {
        return dashscopeBaseUrl;
    }

    public void setDashscopeBaseUrl(String dashscopeBaseUrl) {
        this.dashscopeBaseUrl = dashscopeBaseUrl;
    }

    public String getQdrantUrl() {
        return qdrantUrl;
    }

    public void setQdrantUrl(String qdrantUrl) {
        this.qdrantUrl = qdrantUrl;
    }

    public String getQdrantCollection() {
        return qdrantCollection;
    }

    public void setQdrantCollection(String qdrantCollection) {
        this.qdrantCollection = qdrantCollection;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
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

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getEmbeddingBatchSize() {
        return embeddingBatchSize;
    }

    public void setEmbeddingBatchSize(int embeddingBatchSize) {
        this.embeddingBatchSize = embeddingBatchSize;
    }

    public boolean isSeedHelpOnStartup() {
        return seedHelpOnStartup;
    }

    public void setSeedHelpOnStartup(boolean seedHelpOnStartup) {
        this.seedHelpOnStartup = seedHelpOnStartup;
    }

    public boolean useMemoryStore() {
        String url = qdrantUrl == null ? "" : qdrantUrl.trim();
        return url.isEmpty() || "memory".equalsIgnoreCase(url) || ":memory:".equals(url);
    }
}

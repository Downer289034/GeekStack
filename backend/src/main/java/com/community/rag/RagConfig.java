package com.community.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 模块装配。
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingClient.class)
    public EmbeddingClient embeddingClient(RagProperties properties, ObjectMapper objectMapper) {
        return new DashScopeEmbeddingClient(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(RagVectorStore.class)
    public RagVectorStore ragVectorStore(RagProperties properties, ObjectMapper objectMapper) {
        if (properties.useMemoryStore()) {
            return new InMemoryRagVectorStore();
        }
        return new QdrantRagVectorStore(properties, objectMapper);
    }
}

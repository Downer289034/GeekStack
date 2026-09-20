package com.community.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * RAG 模块装配。
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    @Bean
    @ConditionalOnMissingBean(EmbeddingClient.class)
    public EmbeddingClient embeddingClient(RagProperties properties, ObjectMapper objectMapper,
                                           Environment environment) {
        if (dotenvEnabled(environment) && isBlank(properties.getDashscopeApiKey())) {
            String key = firstNonBlank(System.getenv("DASHSCOPE_API_KEY"),
                    readRootEnv("DASHSCOPE_API_KEY"));
            if (!isBlank(key)) {
                properties.setDashscopeApiKey(key);
            }
        }
        return new DashScopeEmbeddingClient(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(RagVectorStore.class)
    public RagVectorStore ragVectorStore(RagProperties properties, Environment environment,
                                         ObjectMapper objectMapper) {
        // ${QDRANT_URL:memory} 与 spring.config.import 同文档会提前落成 memory；
        // 进程残留 QDRANT_URL=:memory: 还会压过根 .env 的 HTTP。仅在仍为 memory 且允许 dotenv 时回填。
        if (dotenvEnabled(environment) && properties.useMemoryStore()) {
            String resolved = firstHttpUrl(
                    httpOrNull(System.getenv("QDRANT_URL")),
                    readRootEnv("QDRANT_URL"));
            if (resolved != null) {
                properties.setQdrantUrl(resolved);
            }
        }
        if (dotenvEnabled(environment)) {
            String collection = firstNonBlank(System.getenv("QDRANT_COLLECTION"),
                    readRootEnv("QDRANT_COLLECTION"));
            if (!isBlank(collection)) {
                properties.setQdrantCollection(collection.trim());
            }
        }
        log.info("RAG vector store={}", properties.useMemoryStore() ? "memory" : properties.getQdrantUrl());
        if (properties.useMemoryStore()) {
            return new InMemoryRagVectorStore();
        }
        return new QdrantRagVectorStore(properties, objectMapper);
    }

    static boolean dotenvEnabled(Environment environment) {
        return !"false".equalsIgnoreCase(
                environment.getProperty("community.rag.dotenv-qdrant", "true"));
    }

    static String httpOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return null;
    }

    static String firstHttpUrl(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            String http = httpOrNull(candidate);
            if (http != null) {
                return http;
            }
        }
        return null;
    }

    static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (!isBlank(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static String readRootEnv(String key) {
        for (Path path : new Path[]{Path.of(".env"), Path.of("..", ".env")}) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int eq = trimmed.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    if (key.equals(trimmed.substring(0, eq).trim())) {
                        return trimmed.substring(eq + 1).trim();
                    }
                }
            } catch (IOException e) {
                log.warn("读取 {} 失败: {}", path.toAbsolutePath().normalize(), e.getMessage());
            }
        }
        return null;
    }
}

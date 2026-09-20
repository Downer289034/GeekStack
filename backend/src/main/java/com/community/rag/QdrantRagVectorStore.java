package com.community.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Qdrant HTTP 客户端（upsert / 按 parent_content_id 删除）。
 */
public class QdrantRagVectorStore implements RagVectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantRagVectorStore.class);

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public QdrantRagVectorStore(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public void ensureCollection() {
        String base = trimSlash(properties.getQdrantUrl());
        String collection = properties.getQdrantCollection();
        try {
            restTemplate.getForEntity(base + "/collections/" + collection, String.class);
        } catch (RestClientException e) {
            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            "size", properties.getEmbeddingDim(),
                            "distance", "Cosine"));
            putJson(base + "/collections/" + collection, body);
            log.info("RAG Qdrant collection created: {}", collection);
        }
    }

    @Override
    public void upsert(List<RagPointView> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        ensureCollection();
        List<Map<String, Object>> pts = points.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", Math.abs(p.pointId().hashCode()));
            m.put("vector", p.vector());
            m.put("payload", p.payload());
            return m;
        }).toList();
        putJson(collectionUrl() + "/points?wait=true", Map.of("points", pts));
    }

    @Override
    public void deleteByParentContentId(String parentContentId) {
        ensureCollection();
        Map<String, Object> filter = Map.of(
                "must", List.of(Map.of(
                        "key", "parent_content_id",
                        "match", Map.of("value", parentContentId))));
        postJson(collectionUrl() + "/points/delete?wait=true", Map.of("filter", filter));
    }

    @Override
    public int countByParentContentId(String parentContentId) {
        // 生产路径不依赖此方法；返回 -1 表示未实现精确计数
        return -1;
    }

    @Override
    public Map<String, Object> findOnePayloadByParent(String parentContentId) {
        return Map.of();
    }

    private String collectionUrl() {
        return trimSlash(properties.getQdrantUrl()) + "/collections/" + properties.getQdrantCollection();
    }

    private void putJson(String url, Object body) {
        exchange(url, HttpMethod.PUT, body);
    }

    private void postJson(String url, Object body) {
        exchange(url, HttpMethod.POST, body);
    }

    private void exchange(String url, HttpMethod method, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.exchange(url, method, new HttpEntity<>(json, headers), String.class);
        } catch (RestClientException e) {
            log.warn("RAG Qdrant {} 失败 url={} error={}", method, url, e.getMessage());
        } catch (Exception e) {
            log.warn("RAG Qdrant 序列化失败: {}", e.getMessage());
        }
    }

    private static String trimSlash(String base) {
        if (base == null || base.isBlank()) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}

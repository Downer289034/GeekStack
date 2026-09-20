package com.community.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * DashScope text-embedding-v3（OpenAI 兼容）。失败重试 + 降级 WARN，不阻塞主流程。
 */
public class DashScopeEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingClient.class);

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public DashScopeEmbeddingClient(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<List<Float>> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        String key = properties.getDashscopeApiKey();
        if (key == null || key.isBlank()) {
            log.warn("RAG embedding 降级: DASHSCOPE_API_KEY 未配置，使用确定性伪向量");
            return hashVectors(texts);
        }
        List<List<Float>> out = new ArrayList<>(texts.size());
        int batch = Math.max(1, Math.min(properties.getEmbeddingBatchSize(), 10));
        for (int i = 0; i < texts.size(); i += batch) {
            List<String> slice = texts.subList(i, Math.min(texts.size(), i + batch));
            out.addAll(embedBatchWithRetry(slice));
        }
        return out;
    }

    private List<List<Float>> embedBatchWithRetry(List<String> batch) {
        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        RestClientException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return callApi(batch);
            } catch (RestClientException e) {
                last = e;
                log.warn("RAG embedding 失败 attempt={}/{} size={} error={}",
                        attempt, attempts, batch.size(), e.getMessage());
            }
        }
        log.warn("RAG embedding 降级为伪向量: {}", last == null ? "unknown" : last.getMessage());
        return hashVectors(batch);
    }

    private List<List<Float>> callApi(List<String> batch) {
        String url = trimSlash(properties.getDashscopeBaseUrl()) + "/embeddings";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getDashscopeApiKey());
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getEmbeddingModel());
        body.put("input", batch);
        body.put("dimensions", properties.getEmbeddingDim());
        body.put("encoding_format", "float");
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RestClientException("serialize embedding request failed", e);
        }
        String resp = restTemplate.postForObject(url, new HttpEntity<>(json, headers), String.class);
        if (resp == null || resp.isBlank()) {
            throw new RestClientException("empty embedding response");
        }
        try {
            JsonNode root = objectMapper.readTree(resp.getBytes(StandardCharsets.UTF_8));
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new RestClientException("embedding data missing");
            }
            List<JsonNode> nodes = new ArrayList<>();
            data.forEach(nodes::add);
            nodes.sort(Comparator.comparingInt(n -> n.path("index").asInt(0)));
            List<List<Float>> vectors = new ArrayList<>();
            for (JsonNode node : nodes) {
                List<Float> vec = new ArrayList<>();
                for (JsonNode v : node.path("embedding")) {
                    vec.add((float) v.asDouble());
                }
                if (vec.size() != properties.getEmbeddingDim()) {
                    throw new RestClientException("embedding dim mismatch: " + vec.size());
                }
                vectors.add(vec);
            }
            if (vectors.size() != batch.size()) {
                throw new RestClientException("embedding count mismatch");
            }
            return vectors;
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RestClientException("parse embedding response failed", e);
        }
    }

    /** 无 Key / 失败时的确定性伪向量（测试可区分不同文本）。 */
    List<List<Float>> hashVectors(List<String> texts) {
        List<List<Float>> list = new ArrayList<>(texts.size());
        for (String text : texts) {
            list.add(hashVector(text, properties.getEmbeddingDim()));
        }
        return list;
    }

    static List<Float> hashVector(String text, int dim) {
        List<Float> v = new ArrayList<>(dim);
        int h = text == null ? 0 : text.hashCode();
        for (int d = 0; d < dim; d++) {
            h = 31 * h + d;
            v.add(((h % 1000) / 1000f) - 0.5f);
        }
        return v;
    }

    private static String trimSlash(String base) {
        if (base == null || base.isBlank()) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}

package com.community.ai;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 转发 ai-service {@code POST /v1/ask/stream}，SSE 原样透传。
 * 连接失败或上游 5xx：抛 503，由控制器返回降级 JSON（尚未开始写流）。
 */
@Component
public class AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayClient.class);
    private static final Logger COST = LoggerFactory.getLogger("AI_COST");

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiGatewayClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(200, properties.getConnectTimeoutMs())))
                .build();
    }

    /**
     * 打开上游 SSE。失败抛 503，此时尚未向客户端写任何字节。
     */
    public InputStream openStream(String question, String thinking, Integer topK) {
        HttpResponse<InputStream> upstream;
        try {
            String body = objectMapper.writeValueAsString(payload(question, thinking, topK));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.getBaseUrl()) + "/v1/ask/stream"))
                    .timeout(Duration.ofMillis(Math.max(1000, properties.getReadTimeoutMs())))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            upstream = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            log.warn("AI 上游不可达: {}", e.getMessage());
            throw unavailable();
        }
        int status = upstream.statusCode();
        if (status < 200 || status >= 300) {
            closeQuietly(upstream.body());
            log.warn("AI 上游异常状态: {}", status);
            throw unavailable();
        }
        return upstream.body();
    }

    public void copyStream(InputStream upstream, OutputStream clientOut, String username) {
        try (InputStream in = upstream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String event = "";
            String line;
            while ((line = reader.readLine()) != null) {
                clientOut.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                clientOut.flush();
                if (line.isEmpty()) {
                    event = "";
                } else if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:") && "done".equals(event)) {
                    logCost(username, line.substring("data:".length()).trim());
                }
            }
        } catch (Exception e) {
            log.warn("AI SSE 中断: user={}, error={}", username, e.getMessage());
        }
    }

    private Map<String, Object> payload(String question, String thinking, Integer topK) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question);
        body.put("thinking", normalizeThinking(thinking));
        if (topK != null) {
            body.put("top_k", topK);
        }
        return body;
    }

    static String normalizeThinking(String raw) {
        if (raw == null) {
            return "disabled";
        }
        String value = raw.trim().toLowerCase();
        return "enabled".equals(value) ? "enabled" : "disabled";
    }

    private void logCost(String username, String dataJson) {
        try {
            JsonNode usage = objectMapper.readTree(dataJson).path("usage");
            COST.info("user={} promptTokens={} completionTokens={} totalTokens={}",
                    username,
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0),
                    usage.path("total_tokens").asInt(0));
        } catch (Exception e) {
            log.warn("AI 成本日志解析失败: {}", e.getMessage());
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 助手暂时不可用，请稍后再试");
    }

    private static String trimSlash(String base) {
        if (base == null || base.isBlank()) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (Exception ignored) {
            // 上游错误体无需保留
        }
    }
}

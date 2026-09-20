package com.community.ai;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/v1/ai/ask}：JWT（SecurityConfig）→ 限流 → 清洗 → SSE 透传。
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiAskController {

    private final AiAskQuota quota;
    private final AiGatewayClient gatewayClient;

    public AiAskController(AiAskQuota quota, AiGatewayClient gatewayClient) {
        this.quota = quota;
        this.gatewayClient = gatewayClient;
    }

    /**
     * 先连上游再写响应头，连接失败才能返回 JSON 503。
     */
    @PostMapping("/ask")
    public void ask(@Valid @RequestBody AiAskRequest request, Authentication authentication,
                    HttpServletResponse response) throws java.io.IOException {
        quota.consume(authentication.getName());
        String question = request.question().trim();
        InputStream upstream = gatewayClient.openStream(question, request.thinking(), request.topK());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        gatewayClient.copyStream(upstream, response.getOutputStream(), authentication.getName());
    }
}

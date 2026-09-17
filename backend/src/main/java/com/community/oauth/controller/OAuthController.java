package com.community.oauth.controller;

import com.community.common.Result;
import com.community.oauth.service.OAuthService;
import com.community.oauth.service.OAuthStateService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth 接口：
 * - /login/{provider}：302 跳转平台授权页（签发/接纳 state）
 * - /callback/{provider}：平台回调，存 code+state 后 redirect 前端
 * - /{provider}/code：前端转交 code+state（校验即消费）
 * - /unbind/{provider}：解绑（需认证）
 */
@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final OAuthStateService oAuthStateService;
    private final String gitHubClientId;
    private final String giteeClientId;
    private final String redirectBaseUrl;

    public OAuthController(OAuthService oAuthService,
                           OAuthStateService oAuthStateService,
                           @Value("${community.oauth.github.client-id}") String gitHubClientId,
                           @Value("${community.oauth.gitee.client-id}") String giteeClientId,
                           @Value("${community.oauth.redirect-base-url}") String redirectBaseUrl) {
        this.oAuthService = oAuthService;
        this.oAuthStateService = oAuthStateService;
        this.gitHubClientId = gitHubClientId;
        this.giteeClientId = giteeClientId;
        this.redirectBaseUrl = redirectBaseUrl;
    }

    private String buildRedirectUri(String provider) {
        return redirectBaseUrl + "/api/v1/oauth/callback/" + provider;
    }

    private String buildFrontendCallbackUrl(String provider) {
        return redirectBaseUrl + "/oauth/callback/" + provider;
    }

    /**
     * 302 跳转平台授权页。可选 query {@code state}（≥32 hex）由前端生成并入库；
     * 未传则后端 SecureRandom 签发。
     */
    @GetMapping("/login/{provider}")
    public void redirectToAuthorization(@PathVariable String provider,
                                        @RequestParam(required = false) String state,
                                        HttpServletResponse response) throws IOException {
        String normalized = normalizeProvider(provider);
        String issued = oAuthStateService.issue(normalized, state);
        String redirectUri = buildRedirectUri(normalized.toLowerCase());
        String url = switch (normalized) {
            case "GITHUB" -> "https://github.com/login/oauth/authorize"
                    + "?client_id=" + urlEncode(gitHubClientId)
                    + "&scope=" + urlEncode("read:user user:email")
                    + "&redirect_uri=" + urlEncode(redirectUri)
                    + "&state=" + urlEncode(issued);
            case "GITEE" -> "https://gitee.com/oauth/authorize"
                    + "?client_id=" + urlEncode(giteeClientId)
                    + "&scope=" + urlEncode("user_info projects")
                    + "&redirect_uri=" + urlEncode(redirectUri)
                    + "&response_type=code"
                    + "&state=" + urlEncode(issued);
            default -> throw new IllegalArgumentException("不支持的 OAuth 平台");
        };
        response.sendRedirect(url);
    }

    /**
     * 平台回调：将 code+state 写入 localStorage 后 redirect 到前端 OAuth 回调路由。
     */
    @GetMapping("/callback/{provider}")
    public String callback(@PathVariable String provider,
                           @RequestParam(required = false) String code,
                           @RequestParam(required = false) String state) {
        String safeProvider = provider == null ? "" : provider.trim().toLowerCase();
        String safeCode = code == null ? "" : code;
        String safeState = state == null ? "" : state;
        String storeKey = "oauth_pending_" + safeProvider;
        String frontendUrl = buildFrontendCallbackUrl(safeProvider);
        return "<!doctype html><html><body><script>"
                + "localStorage.setItem('" + storeKey + "',"
                + "JSON.stringify({provider:'" + escape(safeProvider)
                + "',code:'" + escape(safeCode)
                + "',state:'" + escape(safeState) + "'}));"
                + "window.location.replace('" + frontendUrl + "');"
                + "</script><p>正在跳转，请稍候…</p></body></html>";
    }

    /** 前端转交授权码：须带 state；Authorization 头存在=绑定语义。 */
    @PostMapping("/{provider}/code")
    public Result<?> submitCode(@PathVariable String provider,
                                @jakarta.validation.Valid @RequestBody CodePayload payload,
                                Authentication authentication) {
        String normalized = normalizeProvider(provider);
        oAuthStateService.consumeOrThrow(payload.state(), normalized);
        String viewer = authentication == null || "anonymousUser".equals(authentication.getName())
                ? null : authentication.getName();
        return Result.ok(oAuthService.handleCallback(normalized, payload.code(), viewer));
    }

    @PostMapping("/unbind/{provider}")
    public Result<Void> unbind(@PathVariable String provider, Authentication authentication) {
        oAuthService.unbind(normalizeProvider(provider), authentication.getName());
        return Result.ok();
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toUpperCase();
        if (!"GITHUB".equals(normalized) && !"GITEE".equals(normalized)) {
            throw new IllegalArgumentException("不支持的 OAuth 平台");
        }
        return normalized;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("<", "\\u003c");
    }

    /**
     * 授权码载荷。state 不做 @NotBlank（缺失走 401 统一口径，非 400）。
     */
    public record CodePayload(
            @jakarta.validation.constraints.NotBlank String code,
            String state
    ) {}
}

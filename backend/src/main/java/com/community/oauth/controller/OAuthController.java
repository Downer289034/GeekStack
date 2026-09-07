package com.community.oauth.controller;

import com.community.common.Result;
import com.community.oauth.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
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
 * - /login/{provider}：302 跳转平台授权页（按钮 href 用此端点）
 * - /callback/{provider}：平台回调，存 code 到 localStorage 后 redirect 到前端路由
 * - /{provider}/code：前端转交 code，完成登录/绑定（POST，匿名可达）
 * - /unbind/{provider}：解绑（需认证）
 */
@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final String gitHubClientId;
    private final String giteeClientId;
    private final String redirectBaseUrl;

    public OAuthController(OAuthService oAuthService,
                           @Value("${community.oauth.github.client-id}") String gitHubClientId,
                           @Value("${community.oauth.gitee.client-id}") String giteeClientId,
                           @Value("${community.oauth.redirect-base-url}") String redirectBaseUrl) {
        this.oAuthService = oAuthService;
        this.gitHubClientId = gitHubClientId;
        this.giteeClientId = giteeClientId;
        this.redirectBaseUrl = redirectBaseUrl;
    }

    // ==================== redirect_uri 构建（唯一出处，消灭硬编码） ====================

    private String buildRedirectUri(String provider) {
        return redirectBaseUrl + "/api/v1/oauth/callback/" + provider;
    }

    private String buildFrontendCallbackUrl(String provider) {
        return redirectBaseUrl + "/oauth/callback/" + provider;
    }

    // ==================== 发起授权 ====================

    /** 302 跳转到平台授权页（按钮 href 直接用此端点）。 */
    @GetMapping("/login/{provider}")
    public void redirectToAuthorization(@PathVariable String provider,
                                        HttpServletResponse response) throws IOException {
        String normalized = normalizeProvider(provider);
        String redirectUri = buildRedirectUri(normalized.toLowerCase());
        String url = switch (normalized) {
            case "GITHUB" -> "https://github.com/login/oauth/authorize"
                    + "?client_id=" + urlEncode(gitHubClientId)
                    + "&scope=" + urlEncode("read:user user:email")
                    + "&redirect_uri=" + urlEncode(redirectUri);
            case "GITEE" -> "https://gitee.com/oauth/authorize"
                    + "?client_id=" + urlEncode(giteeClientId)
                    + "&scope=" + urlEncode("user_info projects")
                    + "&redirect_uri=" + urlEncode(redirectUri)
                    + "&response_type=code";
            default -> throw new IllegalArgumentException("不支持的 OAuth 平台");
        };
        response.sendRedirect(url);
    }

    // ==================== 平台回调（后端 → 前端路由中转） ====================

    /**
     * 平台回调：将 code 写入 localStorage 后 redirect 到前端 OAuth 回调路由。
     * localStorage 写在 redirectBaseUrl 的源上，前端路由同源读取。
     */
    @GetMapping("/callback/{provider}")
    public String callback(@PathVariable String provider, @RequestParam(required = false) String code) {
        String safeProvider = provider == null ? "" : provider.trim().toLowerCase();
        String safeCode = code == null ? "" : code;
        String storeKey = "oauth_pending_" + safeProvider;
        String frontendUrl = buildFrontendCallbackUrl(safeProvider);
        return "<!doctype html><html><body><script>"
                + "localStorage.setItem('" + storeKey + "',"
                + "JSON.stringify({provider:'" + escape(safeProvider) + "',code:'" + escape(safeCode) + "'}));"
                + "window.location.replace('" + frontendUrl + "');"
                + "</script><p>正在跳转，请稍候…</p></body></html>";
    }

    // ==================== 前端转交 code ====================

    /** 前端转交授权码：Authorization 头存在=绑定语义，否则=登录/注册语义。 */
    @PostMapping("/{provider}/code")
    public Result<?> submitCode(@PathVariable String provider,
                                @jakarta.validation.Valid @RequestBody CodePayload payload,
                                Authentication authentication) {
        String viewer = authentication == null || "anonymousUser".equals(authentication.getName())
                ? null : authentication.getName();
        return Result.ok(oAuthService.handleCallback(normalizeProvider(provider), payload.code(), viewer));
    }

    // ==================== 解绑 ====================

    /** 解绑（需认证）。 */
    @PostMapping("/unbind/{provider}")
    public Result<Void> unbind(@PathVariable String provider, Authentication authentication) {
        oAuthService.unbind(normalizeProvider(provider), authentication.getName());
        return Result.ok();
    }

    // ==================== 工具方法 ====================

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

    /** 授权码载荷。 */
    public record CodePayload(@jakarta.validation.constraints.NotBlank String code) {}
}

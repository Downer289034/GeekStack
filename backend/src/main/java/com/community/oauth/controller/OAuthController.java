package com.community.oauth.controller;

import com.community.common.Result;
import com.community.oauth.service.GitHubOAuthService;
import com.community.oauth.service.GiteeOAuthService;
import com.community.oauth.service.OAuthService;
import com.community.user.dto.TokenResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth 接口：回调公开（双语义：带 JWT=绑定，不带=登录/注册）；解绑需认证。
 * 回调双路由设计：
 * - GET /api/v1/oauth/callback/{provider}?code=..：平台直接回调（浏览器会被重定向到此前端页做收尾）
 * - POST /api/v1/oauth/{provider}/code {code}：前端把 code 转交后端（SPA 主路径，未登录=登录/已登录=绑定）
 */
@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final GitHubOAuthService gitHubOAuthService;
    private final GiteeOAuthService giteeOAuthService;
    private final String gitHubClientId;
    private final String giteeClientId;

    public OAuthController(OAuthService oAuthService, GitHubOAuthService gitHubOAuthService,
                           GiteeOAuthService giteeOAuthService,
                           @org.springframework.beans.factory.annotation.Value("${community.oauth.github.client-id}") String gitHubClientId,
                           @org.springframework.beans.factory.annotation.Value("${community.oauth.gitee.client-id}") String giteeClientId) {
        this.oAuthService = oAuthService;
        this.gitHubOAuthService = gitHubOAuthService;
        this.giteeOAuthService = giteeOAuthService;
        this.gitHubClientId = gitHubClientId;
        this.giteeClientId = giteeClientId;
    }

    /** 发起授权：返回平台授权页 URL（前端 fetch 后弹窗/跳转）。 */
    @GetMapping("/authorize-url/{provider}")
    public Result<Map<String, String>> authorizeUrl(@PathVariable String provider) {
        String normalized = provider == null ? "" : provider.trim().toUpperCase();
        String url;
        switch (normalized) {
            case OAuthService.GITHUB -> url = "https://github.com/login/oauth/authorize"
                    + "?client_id=" + urlEncode(gitHubClientIdValue())
                    + "&scope=" + urlEncode("read:user user:email")
                    + "&redirect_uri=" + urlEncode("http://localhost:8080/api/v1/oauth/callback/github");
            case OAuthService.GITEE -> url = "https://gitee.com/oauth/authorize"
                    + "?client_id=" + urlEncode(giteeClientIdValue())
                    + "&scope=" + urlEncode("user_info projects")
                    + "&redirect_uri=" + urlEncode("http://localhost:8080/api/v1/oauth/callback/gitee")
                    + "&response_type=code";
            default -> throw new IllegalArgumentException("不支持的 OAuth 平台");
        }
        return Result.ok(Map.of("authorizeUrl", url));
    }

    /** 直接跳转到平台授权页（按钮 href 用此端点，比弹窗可靠）。 */
    @GetMapping("/login/{provider}")
    public void redirectToAuthorization(@PathVariable String provider,
                                        jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String normalized = provider == null ? "" : provider.trim().toUpperCase();
        String redirectUri = "http://localhost:8080/api/v1/oauth/callback/" + normalized.toLowerCase();
        String url;
        switch (normalized) {
            case OAuthService.GITHUB -> url = "https://github.com/login/oauth/authorize"
                    + "?client_id=" + urlEncode(gitHubClientIdValue())
                    + "&scope=" + urlEncode("read:user user:email")
                    + "&redirect_uri=" + urlEncode(redirectUri);
            case OAuthService.GITEE -> url = "https://gitee.com/oauth/authorize"
                    + "?client_id=" + urlEncode(giteeClientIdValue())
                    + "&scope=" + urlEncode("user_info projects")
                    + "&redirect_uri=" + urlEncode(redirectUri)
                    + "&response_type=code";
            default -> throw new IllegalArgumentException("不支持的 OAuth 平台");
        }
        response.sendRedirect(url);
    }

    /** 前端转交授权码（SPA 主路径）：Authorization 头存在=绑定语义，否则=登录/注册语义。 */
    @PostMapping("/{provider}/code")
    public Result<TokenResponse> submitCode(@PathVariable String provider,
                                            @Valid @RequestBody CodePayload payload,
                                            Authentication authentication) {
        String viewer = authentication == null || "anonymousUser".equals(authentication.getName())
                ? null
                : authentication.getName();
        return Result.ok(oAuthService.handleCallback(provider, payload.code(), viewer));
    }

    /**
     * 平台浏览器回调（302 落地点）：将 code 写入 localStorage 后 redirect 到前端 OAuth 回调路由。
     * 前端路由 OAuthCallbackView 轮询 localStorage 读取 code 并完成登录。
     * localStorage 在新窗口中可靠工作（无 popup 跨域竞态问题）。
     */
    @GetMapping("/callback/{provider}")
    public String callback(@PathVariable String provider, @RequestParam(required = false) String code) {
        String safeProvider = provider == null ? "" : provider.trim().toLowerCase();
        String safeCode = code == null ? "" : code;
        String storeKey = "oauth_pending_" + safeProvider;
        String frontendUrl = "/oauth/callback/" + safeProvider;
        return "<!doctype html><html><body><script>"
                + "localStorage.setItem('" + storeKey + "',"
                + "JSON.stringify({provider:'" + escape(safeProvider) + "',code:'" + escape(safeCode) + "'}));"
                + "window.location.replace('" + frontendUrl + "');"
                + "</script><p>正在跳转，请稍候…</p></body></html>";
    }

    /** 解绑（需认证）。 */
    @PostMapping("/unbind/{provider}")
    public Result<Void> unbind(@PathVariable String provider, Authentication authentication) {
        oAuthService.unbind(provider, authentication.getName());
        return Result.ok();
    }

    private String gitHubClientIdValue() {
        return gitHubClientId;
    }

    private String giteeClientIdValue() {
        return giteeClientId;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("<", "\\u003c");
    }

    /** 授权码载荷。 */
    public record CodePayload(@NotBlank String code) {
    }
}

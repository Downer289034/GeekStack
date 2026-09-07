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

    /** 发起授权：返回平台授权页 URL（前端跳转/开窗）。state 防 CSRF 由平台 state 参数承担（回调原样带回）。 */
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
     * 平台浏览器回调（302 落地点）：向浏览器返回极简收尾页，通过双通道把 code 回传给父窗口。
     * 通道 1：opener.postMessage（opener 存在时走此路径）
     * 通道 2：localStorage 标记（opener 被浏览器安全策略置 null 时的降级方案）
     */
    @GetMapping("/callback/{provider}")
    public String callback(@PathVariable String provider, @RequestParam(required = false) String code) {
        String safeProvider = provider == null ? "" : provider.trim().toLowerCase();
        String safeCode = code == null ? "" : code;
        String storeKey = "oauth_pending_" + safeProvider;
        return "<!doctype html><html><body><script>"
                + "(function(){"
                + "var payload=JSON.stringify({provider:'" + escape(safeProvider) + "',code:'" + escape(safeCode) + "'});"
                // 通道 1：opener.postMessage
                + "try{if(window.opener){window.opener.postMessage(payload,'*');}}catch(e){}"
                // 通道 2：localStorage 标记（父窗口轮询 / 从 storage 事件感知）
                + "try{localStorage.setItem('" + storeKey + "',payload);}catch(e){}"
                + "setTimeout(function(){try{window.close();}catch(e){}},300);"
                + "})()"
                + "</script><p>授权完成，请返回页面</p></body></html>";
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

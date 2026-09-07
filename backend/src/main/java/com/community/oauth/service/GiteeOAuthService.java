package com.community.oauth.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.oauth.dto.OAuthUserInfo;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Gitee OAuth 授权码换 Token + 拉用户信息（scope 最小化：user_info projects）。
 * Gitee token 有过期时间（expires_in 秒），refresh_token 由入库保存（T16 刷新用）。
 */
@Service
public class GiteeOAuthService {

    private static final String TOKEN_URL = "https://gitee.com/oauth/token";
    private static final String USER_URL = "https://gitee.com/api/v5/user";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestClient restClient;

    public GiteeOAuthService(@Value("${community.oauth.gitee.client-id}") String clientId,
                             @Value("${community.oauth.gitee.client-secret}") String clientSecret,
                             @Value("${community.oauth.redirect-base-url}") String redirectBaseUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectBaseUrl + "/api/v1/oauth/callback/gitee";
        this.restClient = RestClient.create();
    }

    /** 用授权码换 access token；返回 access token，并传出刷新信息。 */
    @SuppressWarnings("unchecked")
    public TokenExchangeResult exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", redirectUri);
        Map<String, Object> body = restClient.post()
                .uri(uriBuilder -> uriBuilder.scheme("https").host("gitee.com").path("/oauth/token").build())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (body == null || body.get("access_token") == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Gitee 授权失败");
        }
        LocalDateTime expiresAt = body.get("expires_in") == null
                ? null
                : LocalDateTime.now().plusSeconds(((Number) body.get("expires_in")).longValue());
        return new TokenExchangeResult(
                (String) body.get("access_token"),
                (String) body.get("refresh_token"),
                expiresAt);
    }

    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String accessToken) {
        Map<String, Object> user = restClient.get()
                .uri(USER_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        if (user == null || user.get("id") == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "获取 Gitee 用户信息失败");
        }
        String login = (String) user.get("login");
        return new OAuthUserInfo(
                String.valueOf(user.get("id")),
                login,
                (String) user.get("name") != null ? (String) user.get("name") : login,
                (String) user.get("email"),
                (String) user.get("avatar_url"));
    }

    /** 换 token 结果（access + refresh + 过期时间）。 */
    public record TokenExchangeResult(String accessToken, String refreshToken, LocalDateTime expiresAt) {
    }
}

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
 * GitHub OAuth 授权码换 Token + 拉用户信息（scope 最小化：read:user user:email）。
 * 独立于 T16 的 RepoProvider（本类只管身份，仓库拉取在 T16）。
 */
@Service
public class GitHubOAuthService {

    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";
    private static final String EMAILS_URL = "https://api.github.com/user/emails";

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public GitHubOAuthService(@Value("${community.oauth.github.client-id}") String clientId,
                              @Value("${community.oauth.github.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.create();
    }

    /** 用授权码换 access token。 */
    @SuppressWarnings("unchecked")
    public String exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        Map<String, Object> body = restClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        String token = body == null ? null : (String) body.get("access_token");
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "GitHub 授权失败");
        }
        return token;
    }

    /** 拉取用户信息；email 取主邮箱（GitHub 用户主页邮箱可能为空，读 emails 接口兜底）。 */
    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String accessToken) {
        Map<String, Object> user = restClient.get()
                .uri(USER_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(Map.class);
        if (user == null || user.get("id") == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "获取 GitHub 用户信息失败");
        }
        String email = (String) user.get("email");
        if (email == null) {
            Object[] emails = restClient.get()
                    .uri(EMAILS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(Object[].class);
            if (emails != null) {
                for (Object item : emails) {
                    Map<String, Object> mail = (Map<String, Object>) item;
                    if (Boolean.TRUE.equals(mail.get("primary"))) {
                        email = (String) mail.get("email");
                        break;
                    }
                }
            }
        }
        return new OAuthUserInfo(
                String.valueOf(user.get("id")),
                (String) user.get("login"),
                (String) user.get("name") != null ? (String) user.get("name") : (String) user.get("login"),
                email,
                (String) user.get("avatar_url"));
    }

    /** Token 过期时间：GitHub 长期 token 无过期，返回 null。 */
    public LocalDateTime expiresAt() {
        return null;
    }
}

package com.community.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 签发与校验（HS256）。令牌分两种：typ=access（30 分钟）、typ=refresh（7 天）。
 * 含 ver claim（token_version）：改密后旧 refresh 在续期时失败。
 * 密钥来自环境变量，缺失或不足 32 字节时拒绝启动。
 */
@Component
public class JwtService {

    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_VER = "ver";
    private static final String TYP_ACCESS = "access";
    private static final String TYP_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(@Value("${community.jwt.secret}") String secret,
                      @Value("${community.jwt.access-ttl-minutes}") long accessTtlMinutes,
                      @Value("${community.jwt.refresh-ttl-days}") long refreshTtlDays) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET 未配置或长度不足（至少 32 字节）");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    /** 为指定用户签发访问令牌（带 token_version）。 */
    public String generateAccess(String username, int tokenVersion) {
        return generate(username, TYP_ACCESS, accessTtl, tokenVersion);
    }

    /** 为指定用户签发刷新令牌（带 token_version）。 */
    public String generateRefresh(String username, int tokenVersion) {
        return generate(username, TYP_REFRESH, refreshTtl, tokenVersion);
    }

    /** 校验并解析令牌；过期/签名不符/格式非法抛 JwtException 系列异常。 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** 校验是否为访问令牌（供过滤器使用）。 */
    public boolean isAccessToken(Claims claims) {
        return TYP_ACCESS.equals(claims.get(CLAIM_TYP, String.class));
    }

    /** 校验是否为刷新令牌（供刷新接口使用）。 */
    public boolean isRefreshToken(Claims claims) {
        return TYP_REFRESH.equals(claims.get(CLAIM_TYP, String.class));
    }

    /**
     * 读取 ver claim；旧令牌无该字段时视为 0（与库默认 token_version 对齐）。
     */
    public int tokenVersion(Claims claims) {
        Integer ver = claims.get(CLAIM_VER, Integer.class);
        return ver == null ? 0 : ver;
    }

    private String generate(String username, String typ, Duration ttl, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_TYP, typ)
                .claim(CLAIM_VER, tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }
}

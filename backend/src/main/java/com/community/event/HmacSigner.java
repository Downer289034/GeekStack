package com.community.event;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 签名工具（契约 §2）：
 * signing_string = "<timestamp>" + "." + "<raw_body>"
 * signature = HMAC_SHA256(shared_secret, signing_string) 的 hex 小写
 * 密钥 32 字节，走环境变量（STEWARD_WEBHOOK_SECRET），与 JWT_SECRET 同级红线。
 */
@Component
public class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKey key;

    public HmacSigner(@Value("${community.event.webhook-secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("STEWARD_WEBHOOK_SECRET 未配置");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** 签名：返回 hex 小写。 */
    public String sign(long timestamp, String rawBody) {
        try {
            String signingString = timestamp + "." + rawBody;
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] hash = mac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 签名失败", e);
        }
    }
}

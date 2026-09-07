package com.community.common;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM 加解密（OAuth Token 等敏感凭据的唯一入库方式）。
 * 密文格式：base64(iv(12B) || ciphertext+tag)。密钥来自环境变量，缺失或不足 32 字节拒绝启动。
 * 红线：任何明文 Token 不得入库/入日志（检查点 C 一票否决项）。
 */
@Component
public class AesGcmCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCipher(@Value("${community.oauth.aes-key}") String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("OAUTH_AES_KEY 必须是合法 Base64（解码后 32 字节）", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("OAUTH_AES_KEY 解码后必须为 32 字节（AES-256）");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /** 加密：返回 base64(iv || ciphertext+tag)。 */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    /** 解密：密文被篡改或密钥不符抛 AEADBadTagException（由调用方决定语义）。 */
    public String decrypt(String base64Ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, combined, 0, IV_BYTES));
            byte[] plainBytes = cipher.doFinal(combined, IV_BYTES, combined.length - IV_BYTES);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 解密失败", e);
        }
    }
}

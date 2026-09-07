package com.community.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * AES-GCM 加解密属性测试：往返一致、密文随机化（同明文不同密文）、篡改检测、密钥校验。
 */
class AesGcmCipherTest {

    private AesGcmCipher cipherWithKey(String base64Key) {
        return new AesGcmCipher(base64Key);
    }

    private String randomKeyBase64() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    void encryptDecryptRoundTrip() {
        AesGcmCipher cipher = cipherWithKey(randomKeyBase64());
        String plaintext = "gho_sampleAccessTokenWithChinese支持";
        assertThat(cipher.decrypt(cipher.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    void samePlaintextProducesDifferentCiphertexts() {
        AesGcmCipher cipher = cipherWithKey(randomKeyBase64());
        String ciphertext1 = cipher.encrypt("same-token");
        String ciphertext2 = cipher.encrypt("same-token");
        assertThat(ciphertext1).isNotEqualTo(ciphertext2);
    }

    @Test
    void tamperedCiphertextFailsDecryption() {
        AesGcmCipher cipher = cipherWithKey(randomKeyBase64());
        String ciphertext = cipher.encrypt("secret");
        byte[] bytes = Base64.getDecoder().decode(ciphertext);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyFailsDecryption() {
        String ciphertext = cipherWithKey(randomKeyBase64()).encrypt("secret");
        assertThatThrownBy(() -> cipherWithKey(randomKeyBase64()).decrypt(ciphertext))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsShortKey() {
        byte[] shortKey = new byte[16];
        new java.security.SecureRandom().nextBytes(shortKey);
        String base64 = Base64.getEncoder().encodeToString(shortKey);
        assertThatThrownBy(() -> cipherWithKey(base64)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonBase64Key() {
        assertThatThrownBy(() -> cipherWithKey("not-base64-!!!")).isInstanceOf(IllegalStateException.class);
    }
}

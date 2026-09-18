package com.community.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import org.junit.jupiter.api.Test;

/** ImageValidator 魔数为王策略与场景大小单测。 */
class ImageValidatorTest {

    private static final byte[] PNG_HEADER = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
    };
    private static final byte[] JPEG_HEADER = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final byte[] GIF_HEADER = new byte[] {
            'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0
    };
    private static final byte[] WEBP_HEADER = new byte[] {
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
    };
    private static final byte[] EXE_HEADER = new byte[] {
            'M', 'Z', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    @Test
    void acceptsPngByMagic() {
        var v = ImageValidator.validate(PNG_HEADER);
        assertThat(v.extension()).isEqualTo("png");
        assertThat(v.contentType()).isEqualTo("image/png");
    }

    @Test
    void acceptsJpegAndNormalizesExt() {
        var v = ImageValidator.validate(JPEG_HEADER);
        assertThat(v.extension()).isEqualTo("jpg");
        assertThat(v.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void acceptsFakePngWhenMagicIsWhitelisted() {
        // 名实不符（.png 名 / image/png 声明 / JPEG 内容）→ 按真实格式 JPEG 收
        var v = ImageValidator.validate(JPEG_HEADER);
        assertThat(v.extension()).isEqualTo("jpg");
        assertThat(v.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void rejectsNonImageMagic() {
        assertThatThrownBy(() -> ImageValidator.validate(EXE_HEADER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void rejectsTooShortHeader() {
        assertThatThrownBy(() -> ImageValidator.validate(new byte[] {(byte) 0x89, 0x50}))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void sceneLimits() {
        assertThat(ImageValidator.maxBytesForScene("editor")).isEqualTo(10L * 1024 * 1024);
        assertThat(ImageValidator.maxBytesForScene("cover")).isEqualTo(5L * 1024 * 1024);
        assertThat(ImageValidator.maxBytesForScene("avatar")).isEqualTo(2L * 1024 * 1024);
        assertThatThrownBy(() -> ImageValidator.maxBytesForScene("video"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void acceptsGifAndWebp() {
        assertThat(ImageValidator.validate(GIF_HEADER).extension()).isEqualTo("gif");
        assertThat(ImageValidator.validate(WEBP_HEADER).extension()).isEqualTo("webp");
    }
}

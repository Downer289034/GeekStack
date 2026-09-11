package com.community.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import org.junit.jupiter.api.Test;

/** ImageValidator 三一致与场景大小单测。 */
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

    @Test
    void acceptsPngWithMatchingType() {
        var v = ImageValidator.validate("shot.PNG", "image/png", PNG_HEADER);
        assertThat(v.extension()).isEqualTo("png");
        assertThat(v.contentType()).isEqualTo("image/png");
    }

    @Test
    void acceptsJpegAndNormalizesExt() {
        var v = ImageValidator.validate("a.jpeg", "image/jpeg", JPEG_HEADER);
        assertThat(v.extension()).isEqualTo("jpg");
    }

    @Test
    void rejectsHtmlDisguisedAsPng() {
        assertThatThrownBy(() -> ImageValidator.validate("x.html", "image/png", PNG_HEADER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void rejectsMagicMismatch() {
        assertThatThrownBy(() -> ImageValidator.validate("a.png", "image/png", JPEG_HEADER))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsContentTypeMismatch() {
        assertThatThrownBy(() -> ImageValidator.validate("a.png", "image/jpeg", PNG_HEADER))
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
        assertThat(ImageValidator.validate("a.gif", "image/gif", GIF_HEADER).extension()).isEqualTo("gif");
        assertThat(ImageValidator.validate("a.webp", "image/webp", WEBP_HEADER).extension()).isEqualTo("webp");
    }
}

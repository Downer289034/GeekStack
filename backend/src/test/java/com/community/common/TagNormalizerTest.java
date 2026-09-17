package com.community.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** TagNormalizer 去重与上限。 */
class TagNormalizerTest {

    @Test
    void normalizesTrimDedupCaseInsensitive() {
        assertThat(TagNormalizer.normalize(List.of(" Java ", "AI", "java", "", "  "), 5))
                .containsExactly("Java", "AI");
    }

    @Test
    void emptyWhenNull() {
        assertThat(TagNormalizer.normalize(null, 5)).isEmpty();
    }

    @Test
    void rejectsOverMax() {
        assertThatThrownBy(() -> TagNormalizer.normalize(List.of("a", "b", "c", "d", "e", "f"), 5))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }
}

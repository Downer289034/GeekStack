package com.community.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** LikeEscape 通配符转义单测。 */
class LikeEscapeTest {

    @Test
    void escapesPercentUnderscoreAndBackslash() {
        assertThat(LikeEscape.escape("100%_off\\x")).isEqualTo("100\\%\\_off\\\\x");
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertThat(LikeEscape.escape(null)).isNull();
        assertThat(LikeEscape.escape("")).isEmpty();
    }
}

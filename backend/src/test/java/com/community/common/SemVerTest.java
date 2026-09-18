package com.community.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SemVerTest {

    @Test
    void acceptsCoreSemVer() {
        assertThat(SemVer.isValid("0.0.0")).isTrue();
        assertThat(SemVer.isValid("1.0.0")).isTrue();
        assertThat(SemVer.isValid("1.2.3")).isTrue();
        assertThat(SemVer.isValid("10.20.30")).isTrue();
    }

    @Test
    void rejectsInvalid() {
        assertThat(SemVer.isValid(null)).isFalse();
        assertThat(SemVer.isValid("")).isFalse();
        assertThat(SemVer.isValid("v1.0.0")).isFalse();
        assertThat(SemVer.isValid("1.0")).isFalse();
        assertThat(SemVer.isValid("1.0.0-beta")).isFalse();
        assertThat(SemVer.isValid("01.0.0")).isFalse();
    }
}

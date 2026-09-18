package com.community.common;

import java.util.regex.Pattern;

/**
 * SemVer 核心校验（MAJOR.MINOR.PATCH，不含预发布/构建元数据）。
 */
public final class SemVer {

    private static final Pattern CORE = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

    private SemVer() {
    }

    /** 是否为合法 SemVer 核心版本。 */
    public static boolean isValid(String version) {
        return version != null && CORE.matcher(version.trim()).matches();
    }
}

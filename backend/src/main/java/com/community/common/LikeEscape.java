package com.community.common;

/**
 * LIKE 通配符转义：用户输入中的 {@code %} / {@code _} / {@code \} 必须转义，否则既是注入面也会误匹配。
 */
public final class LikeEscape {

    private LikeEscape() {
    }

    /**
     * 转义后配合 SQL {@code LIKE ... ESCAPE '\\'} 使用（参数化绑定，禁止拼接）。
     */
    public static String escape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

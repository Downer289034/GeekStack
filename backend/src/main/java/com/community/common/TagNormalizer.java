package com.community.common;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 标签归一化：trim、去空、去重（大小写不敏感，保留首次出现的写法），上限校验。
 */
public final class TagNormalizer {

    private TagNormalizer() {
    }

    /**
     * 归一化标签列表。
     *
     * @param tags    原始标签（可为 null）
     * @param maxSize 最大个数（含）
     * @return 不可变列表；null/空 → 空列表
     */
    public static List<String> normalize(List<String> tags, int maxSize) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> seenLower = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String raw : tags) {
            if (raw == null) {
                continue;
            }
            String t = raw.trim();
            if (t.isEmpty()) {
                continue;
            }
            String key = t.toLowerCase(Locale.ROOT);
            if (seenLower.add(key)) {
                result.add(t);
            }
        }
        if (result.size() > maxSize) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签最多 " + maxSize + " 个");
        }
        return List.copyOf(result);
    }
}

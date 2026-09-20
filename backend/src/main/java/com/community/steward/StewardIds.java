package com.community.steward;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;

/**
 * 管家侧脱敏 ID：与事件契约前缀一致（user_ / post_ / comment_ / share_）。
 */
public final class StewardIds {

    private StewardIds() {
    }

    /** {@code user_123}。 */
    public static String user(long id) {
        return "user_" + id;
    }

    /** {@code post_123}。 */
    public static String post(long id) {
        return "post_" + id;
    }

    /** {@code comment_123}。 */
    public static String comment(long id) {
        return "comment_" + id;
    }

    /** {@code share_123}。 */
    public static String share(long id) {
        return "share_" + id;
    }

    /**
     * 解析路径参数：纯数字或 {@code user_{id}}。
     *
     * @throws BusinessException 格式非法时 400
     */
    public static long parseUserKey(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "userKey 不能为空");
        }
        String raw = userKey.trim();
        if (raw.regionMatches(true, 0, "user_", 0, 5)) {
            raw = raw.substring(5);
        }
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "userKey 非法");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "userKey 须为数字或 user_{id}");
        }
    }

    /** 正文截断（调查上下文最小集，不含全量落库）。 */
    public static String snippet(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.strip();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }
}

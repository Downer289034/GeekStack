package com.community.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

/**
 * 统一分页响应（屏蔽 MyBatis-Plus Page 的内部字段，稳定 API 契约）。
 */
public record PageResponse<T>(
        long total,
        long current,
        long size,
        List<T> records
) {

    /** 从 MyBatis-Plus 分页结果转换。 */
    public static <T> PageResponse<T> from(IPage<T> page) {
        return new PageResponse<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}

package com.community.steward.dto;

import java.util.List;

/**
 * 判罚回流一期占位：空 records，{@code placeholder=true} 标明未接线。
 */
public record PenaltyPageResponse(
        long total,
        long current,
        long size,
        List<Object> records,
        boolean placeholder,
        String note
) {
}

package com.community.common;

import lombok.Getter;

/**
 * 统一响应体：code=0 成功；非 0 失败（见 {@link ErrorCode} 的区段规则）。
 */
@Getter
public class Result<T> {

    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功响应，无数据。 */
    public static Result<Void> ok() {
        return ok(null);
    }

    /** 成功响应，携带数据。 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data);
    }

    /** 失败响应，消息取错误码默认文案。 */
    public static Result<Void> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 失败响应，覆盖默认消息。 */
    public static Result<Void> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
}

package com.community.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务错误码：前三位与 HTTP 状态码对应，便于客户端归因。
 * 业务模块新增错误码时延续同一区段规则（如 40901 = CONFLICT 区段的细分）。
 */
@Getter
public enum ErrorCode {

    OK(0, HttpStatus.OK, "ok"),
    PARAM_ERROR(40000, HttpStatus.BAD_REQUEST, "参数错误"),
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),
    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "无权限"),
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(40900, HttpStatus.CONFLICT, "资源冲突"),
    TOO_MANY_REQUESTS(42900, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
    SERVICE_UNAVAILABLE(50300, HttpStatus.SERVICE_UNAVAILABLE, "服务暂不可用"),
    SYSTEM_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}

package com.community.common;

import lombok.Getter;

/**
 * 业务异常：携带 {@link ErrorCode}，由全局异常处理器统一转换为响应体。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 按错误码构造，消息取错误码默认文案。 */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 按错误码构造并覆盖默认消息。 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

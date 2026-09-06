package com.community.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有异常统一转换为 {@link Result} JSON，HTTP 状态码语义正确。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常携带的错误码与消息返回。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return build(e.getErrorCode().getHttpStatus(), Result.fail(e.getErrorCode(), e.getMessage()));
    }

    /** 参数校验失败（@Valid / 表单绑定）：400，返回首个字段错误信息。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleValidation(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null
                ? ErrorCode.PARAM_ERROR.getMessage()
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return build(ErrorCode.PARAM_ERROR.getHttpStatus(), Result.fail(ErrorCode.PARAM_ERROR, message));
    }

    /** 请求体不可读或参数类型不匹配：400。 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleUnreadable() {
        return build(ErrorCode.PARAM_ERROR.getHttpStatus(), Result.fail(ErrorCode.PARAM_ERROR));
    }

    /** 静态资源与路径未命中：404。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound() {
        return build(ErrorCode.NOT_FOUND.getHttpStatus(), Result.fail(ErrorCode.NOT_FOUND));
    }

    /** 未捕获异常：500，不向前端泄漏内部细节。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return build(ErrorCode.SYSTEM_ERROR.getHttpStatus(), Result.fail(ErrorCode.SYSTEM_ERROR));
    }

    private ResponseEntity<Result<Void>> build(HttpStatus status, Result<Void> body) {
        return ResponseEntity.status(status).body(body);
    }
}

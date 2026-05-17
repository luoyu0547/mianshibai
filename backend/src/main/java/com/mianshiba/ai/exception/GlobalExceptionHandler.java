package com.mianshiba.ai.exception;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        // 1. 提取参数校验错误信息
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());

        // 2. 返回统一错误响应
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        // 1. 记录约束校验异常
        log.warn("Constraint violation", exception);

        // 2. 返回统一错误响应
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException exception) {
        // 1. 记录业务异常
        log.warn("Business exception: code={}, message={}", exception.getCode(), exception.getMessage());

        // 2. 返回统一错误响应
        return ResultUtils.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception exception) {
        // 1. 记录未预期异常
        log.error("Unexpected exception", exception);

        // 2. 返回统一错误响应
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}

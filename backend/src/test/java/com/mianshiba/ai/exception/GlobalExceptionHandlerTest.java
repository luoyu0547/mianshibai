package com.mianshiba.ai.exception;

import com.mianshiba.ai.common.BaseResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionReturnsBusinessError() {
        BusinessException exception = new BusinessException(ErrorCode.PARAMS_ERROR, "参数无效");

        BaseResponse<Void> response = globalExceptionHandler.handleBusinessException(exception);

        assertThat(response.getCode()).isEqualTo(40000);
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("参数无效");
    }

    @Test
    void handleExceptionReturnsSystemError() {
        BaseResponse<Void> response = globalExceptionHandler.handleException(new RuntimeException("boom"));

        assertThat(response.getCode()).isEqualTo(50000);
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("系统内部异常");
    }
}

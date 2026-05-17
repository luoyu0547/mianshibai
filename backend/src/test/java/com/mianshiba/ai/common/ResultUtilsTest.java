package com.mianshiba.ai.common;

import com.mianshiba.ai.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultUtilsTest {

    @Test
    void successReturnsStandardResponse() {
        BaseResponse<String> response = ResultUtils.success("ok-data");

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData()).isEqualTo("ok-data");
        assertThat(response.getMessage()).isEqualTo("ok");
    }

    @Test
    void errorReturnsStandardResponse() {
        BaseResponse<Void> response = ResultUtils.error(ErrorCode.PARAMS_ERROR);

        assertThat(response.getCode()).isEqualTo(40000);
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("请求参数错误");
    }
}

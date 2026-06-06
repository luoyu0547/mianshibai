package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.SpeechProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AliyunSpeechServiceImplTest {

    @Test
    void synthesizeToBase64_throwsWhenCredentialsMissing() {
        SpeechProperties props = new SpeechProperties();
        props.getAliyun().setAppKey("");
        AliyunSpeechServiceImpl service = new AliyunSpeechServiceImpl(props);

        assertThatThrownBy(() -> service.synthesizeToBase64("hello"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.SPEECH_SERVICE_ERROR.getCode());
    }

    @Test
    void createAsrStreamSession_throwsWhenCredentialsMissing() {
        SpeechProperties props = new SpeechProperties();
        props.getAliyun().setAppKey("");
        AliyunSpeechServiceImpl service = new AliyunSpeechServiceImpl(props);

        assertThatThrownBy(() -> service.createAsrStreamSession(s -> {}, s -> {}, e -> {}))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.SPEECH_SERVICE_ERROR.getCode());
    }
}

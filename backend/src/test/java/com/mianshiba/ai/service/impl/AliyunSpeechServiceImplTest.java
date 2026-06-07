package com.mianshiba.ai.service.impl;

import com.alibaba.nls.client.protocol.NlsClient;
import com.mianshiba.ai.config.NlsClientManager;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AliyunSpeechServiceImplTest {

    private AliyunSpeechServiceImpl createServiceWithMockClient() {
        NlsClientManager manager = mock(NlsClientManager.class);
        when(manager.getClient()).thenReturn(mock(NlsClient.class));
        when(manager.getAppKey()).thenReturn("test-app-key");
        return new AliyunSpeechServiceImpl(manager);
    }

    @Test
    void synthesizeToBase64_throwsWhenSynthesisFails() {
        NlsClientManager manager = mock(NlsClientManager.class);
        when(manager.getClient()).thenThrow(new IllegalStateException("NlsClient 初始化失败: token error"));
        when(manager.getAppKey()).thenReturn("test-app-key");
        AliyunSpeechServiceImpl service = new AliyunSpeechServiceImpl(manager);

        assertThatThrownBy(() -> service.synthesizeToBase64("hello"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.SPEECH_SERVICE_ERROR.getCode());
    }

    @Test
    void createAsrStreamSession_throwsWhenClientInitFails() {
        NlsClientManager manager = mock(NlsClientManager.class);
        when(manager.getClient()).thenThrow(new IllegalStateException("NlsClient 初始化失败: token error"));
        when(manager.getAppKey()).thenReturn("test-app-key");
        AliyunSpeechServiceImpl service = new AliyunSpeechServiceImpl(manager);

        assertThatThrownBy(() -> service.createAsrStreamSession(s -> {}, s -> {}, e -> {}))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.SPEECH_SERVICE_ERROR.getCode());
    }
}

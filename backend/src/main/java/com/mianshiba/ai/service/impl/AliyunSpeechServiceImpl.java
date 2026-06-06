package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.SpeechProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.SpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSpeechServiceImpl implements SpeechService {

    private final SpeechProperties speechProperties;

    @Override
    public String synthesizeToBase64(String text) {
        if (isCredentialsMissing()) {
            log.error("阿里云语音服务未配置: appKey 为空");
            throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR);
        }
        log.info("TTS synthesize: {} (SDK integration pending)", text.substring(0, Math.min(text.length(), 50)));
        throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR);
    }

    @Override
    public AsrStreamSession createAsrStreamSession(Consumer<String> onPartial,
                                                    Consumer<String> onFinal,
                                                    Consumer<Throwable> onError) {
        if (isCredentialsMissing()) {
            log.error("阿里云语音服务未配置: appKey 为空");
            throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR);
        }
        return new StubAsrStreamSession(onPartial, onFinal, onError);
    }

    private boolean isCredentialsMissing() {
        return speechProperties.getAliyun() == null
                || speechProperties.getAliyun().getAppKey() == null
                || speechProperties.getAliyun().getAppKey().isBlank();
    }

    private static class StubAsrStreamSession implements AsrStreamSession {

        private final Consumer<String> onPartial;
        private final Consumer<String> onFinal;
        private final Consumer<Throwable> onError;

        StubAsrStreamSession(Consumer<String> onPartial, Consumer<String> onFinal, Consumer<Throwable> onError) {
            this.onPartial = onPartial;
            this.onFinal = onFinal;
            this.onError = onError;
        }

        @Override
        public void start() {
        }

        @Override
        public void sendAudio(byte[] audio) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void close() {
        }
    }
}

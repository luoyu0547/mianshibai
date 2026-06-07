package com.mianshiba.ai.service.impl;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import com.mianshiba.ai.config.NlsClientManager;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.SpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSpeechServiceImpl implements SpeechService {

    private static final String TTS_VOICE = "zhixiaoxia";
    private static final int TTS_TIMEOUT_MS = 30_000;

    private final NlsClientManager nlsClientManager;

    private NlsClient safeGetClient() {
        try {
            return nlsClientManager.getClient();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR, "语音服务初始化失败: " + e.getMessage());
        }
    }

    @Override
    public String synthesizeToBase64(String text) {
        NlsClient client = safeGetClient();
        String appKey = nlsClientManager.getAppKey();

        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        CountDownLatch completeLatch = new CountDownLatch(1);
        AtomicReference<String> errorRef = new AtomicReference<>();

        SpeechSynthesizerListener listener = new SpeechSynthesizerListener() {
            @Override
            public void onMessage(ByteBuffer message) {
                byte[] bytes = new byte[message.remaining()];
                message.get(bytes);
                synchronized (audioBuffer) {
                    audioBuffer.writeBytes(bytes);
                }
            }

            @Override
            public void onComplete(SpeechSynthesizerResponse response) {
                log.debug("TTS 合成完成, taskId={}", response.getTaskId());
                completeLatch.countDown();
            }

            @Override
            public void onFail(SpeechSynthesizerResponse response) {
                log.error("TTS 合成失败, taskId={}, status={}, statusText={}",
                        response.getTaskId(), response.getStatus(), response.getStatusText());
                errorRef.set("TTS 失败: " + response.getStatusText());
                completeLatch.countDown();
            }
        };

        SpeechSynthesizer synthesizer = null;
        try {
            synthesizer = new SpeechSynthesizer(client, listener);
            synthesizer.setAppKey(appKey);
            synthesizer.setVoice(TTS_VOICE);
            synthesizer.setFormat(OutputFormatEnum.WAV);
            synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            synthesizer.setSpeechRate(0);
            synthesizer.setVolume(50);
            synthesizer.setText(text);
            synthesizer.start();

            if (!completeLatch.await(TTS_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR, "TTS 合成超时");
            }

            String error = errorRef.get();
            if (error != null) {
                throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR, error);
            }

            byte[] audioBytes;
            synchronized (audioBuffer) {
                audioBytes = audioBuffer.toByteArray();
            }
            if (audioBytes.length == 0) {
                throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR, "TTS 合成结果为空");
            }

            return Base64.getEncoder().encodeToString(audioBytes);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS 调用异常", e);
            throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR, "语音合成失败: " + e.getMessage());
        } finally {
            if (synthesizer != null) {
                synthesizer.close();
            }
        }
    }

    @Override
    public AsrStreamSession createAsrStreamSession(Consumer<String> onPartial,
                                                    Consumer<String> onFinal,
                                                    Consumer<Throwable> onError) {
        NlsClient client = safeGetClient();
        String appKey = nlsClientManager.getAppKey();

        try {
            SpeechTranscriberListener listener = new SpeechTranscriberListener() {
                @Override
                public void onTranscriberStart(SpeechTranscriberResponse response) {
                    log.debug("ASR 开始, taskId={}", response.getTaskId());
                }

                @Override
                public void onSentenceBegin(SpeechTranscriberResponse response) {
                    log.debug("ASR 句子开始, index={}", response.getTransSentenceIndex());
                }

                @Override
                public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                    String result = response.getTransSentenceText();
                    if (result != null && !result.isEmpty()) {
                        onPartial.accept(result);
                    }
                }

                @Override
                public void onSentenceEnd(SpeechTranscriberResponse response) {
                    String result = response.getTransSentenceText();
                    if (result != null && !result.isEmpty()) {
                        onFinal.accept(result);
                    }
                }

                @Override
                public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                    log.debug("ASR 完成, taskId={}", response.getTaskId());
                }

                @Override
                public void onFail(SpeechTranscriberResponse response) {
                    log.error("ASR 失败, taskId={}, status={}, statusText={}",
                            response.getTaskId(), response.getStatus(), response.getStatusText());
                    onError.accept(new RuntimeException("ASR 失败: " + response.getStatusText()));
                }
            };

            SpeechTranscriber transcriber = new SpeechTranscriber(client, listener);
            transcriber.setAppKey(appKey);
            transcriber.setFormat(InputFormatEnum.PCM);
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            transcriber.setEnableIntermediateResult(true);
            transcriber.setEnablePunctuation(true);
            transcriber.setEnableITN(true);

            return new AliyunAsrStreamSession(transcriber);
        } catch (Exception e) {
            log.error("创建 ASR 会话失败", e);
            throw new BusinessException(ErrorCode.SPEECH_SERVICE_ERROR, "创建语音识别会话失败: " + e.getMessage());
        }
    }

    private static class AliyunAsrStreamSession implements AsrStreamSession {

        private final SpeechTranscriber transcriber;
        private volatile boolean started = false;
        private volatile boolean closed = false;

        AliyunAsrStreamSession(SpeechTranscriber transcriber) {
            this.transcriber = transcriber;
        }

        @Override
        public void start() {
            if (closed || started) {
                return;
            }
            try {
                transcriber.start();
                started = true;
            } catch (Exception e) {
                throw new RuntimeException("ASR 启动失败", e);
            }
        }

        @Override
        public void sendAudio(byte[] audio) {
            if (closed || !started) {
                return;
            }
            try {
                transcriber.send(audio);
            } catch (Exception e) {
                throw new RuntimeException("发送音频数据失败", e);
            }
        }

        @Override
        public void stop() {
            if (closed || !started) {
                return;
            }
            try {
                transcriber.stop();
            } catch (Exception e) {
                throw new RuntimeException("ASR 停止失败", e);
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                transcriber.close();
            } catch (Exception e) {
                // close 静默处理
            }
        }
    }
}

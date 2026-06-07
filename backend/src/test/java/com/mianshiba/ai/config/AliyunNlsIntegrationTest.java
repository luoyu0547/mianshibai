package com.mianshiba.ai.config;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@EnabledIfEnvironmentVariable(named = "ALIYUN_AK_ID", matches = ".+")
class AliyunNlsIntegrationTest {

    private static final String AK_ID = System.getenv("ALIYUN_AK_ID");
    private static final String AK_SECRET = System.getenv("ALIYUN_AK_SECRET");
    private static final String APP_KEY = System.getenv("ALIYUN_NLS_APP_KEY");

    @Test
    void testGetToken() throws Exception {
        AccessToken accessToken = new AccessToken(AK_ID, AK_SECRET);
        accessToken.apply();

        assertThat(accessToken.getToken()).isNotBlank();
        assertThat(accessToken.getExpireTime()).isGreaterThan(0);
        System.out.println("Token 获取成功: " + accessToken.getToken().substring(0, 20) + "...");
        System.out.println("过期时间: " + accessToken.getExpireTime());
    }

    @Test
    void testTtsSynthesis() throws Exception {
        AccessToken accessToken = new AccessToken(AK_ID, AK_SECRET);
        accessToken.apply();

        NlsClient client = new NlsClient(
                "wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1",
                accessToken.getToken()
        );

        try {
            ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            CountDownLatch latch = new CountDownLatch(1);
            boolean[] failed = {false};

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
                    System.out.println("TTS 完成, taskId=" + response.getTaskId()
                            + ", status=" + response.getStatus());
                    latch.countDown();
                }

                @Override
                public void onFail(SpeechSynthesizerResponse response) {
                    System.err.println("TTS 失败: taskId=" + response.getTaskId()
                            + ", status=" + response.getStatus()
                            + ", text=" + response.getStatusText());
                    failed[0] = true;
                    latch.countDown();
                }
            };

            SpeechSynthesizer synthesizer = new SpeechSynthesizer(client, listener);
            synthesizer.setAppKey(APP_KEY);
            synthesizer.setVoice("zhixiaoxia");
            synthesizer.setFormat(OutputFormatEnum.WAV);
            synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            synthesizer.setText("你好，这是一条语音合成测试。");
            synthesizer.start();

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(failed[0]).isFalse();

            byte[] audioBytes;
            synchronized (audioBuffer) {
                audioBytes = audioBuffer.toByteArray();
            }
            assertThat(audioBytes.length).isGreaterThan(0);

            String base64 = Base64.getEncoder().encodeToString(audioBytes);
            System.out.println("TTS 成功! 音频大小: " + audioBytes.length + " bytes, Base64 长度: " + base64.length());

            synthesizer.close();
        } finally {
            client.shutdown();
        }
    }

    @Test
    void testNlsClientManagerFlow() {
        SpeechProperties props = new SpeechProperties();
        props.getAliyun().setAccessKeyId(AK_ID);
        props.getAliyun().setAccessKeySecret(AK_SECRET);
        props.getAliyun().setAppKey(APP_KEY);

        NlsClientManager manager = new NlsClientManager(props);

        assertThatCode(() -> {
            NlsClient client = manager.getClient();
            assertThat(client).isNotNull();
            assertThat(manager.getAppKey()).isEqualTo(APP_KEY);
            System.out.println("NlsClientManager 初始化成功, appKey=" + APP_KEY);
            manager.shutdown();
        }).doesNotThrowAnyException();
    }
}

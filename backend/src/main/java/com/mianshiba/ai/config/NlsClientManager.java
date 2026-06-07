package com.mianshiba.ai.config;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.NlsClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class NlsClientManager {

    private final SpeechProperties speechProperties;
    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private volatile long tokenExpireTime = 0;
    private volatile NlsClient nlsClient;

    public NlsClientManager(SpeechProperties speechProperties) {
        this.speechProperties = speechProperties;
    }

    public synchronized NlsClient getClient() {
        if (nlsClient == null || isTokenExpired()) {
            rebuildClient();
        }
        return nlsClient;
    }

    public String getAppKey() {
        return speechProperties.getAliyun().getAppKey();
    }

    private boolean isTokenExpired() {
        return System.currentTimeMillis() >= tokenExpireTime - 60_000;
    }

    private void rebuildClient() {
        SpeechProperties.Aliyun aliyun = speechProperties.getAliyun();

        if (nlsClient != null) {
            try {
                nlsClient.shutdown();
            } catch (Exception e) {
                log.warn("关闭旧 NlsClient 失败", e);
            }
        }

        try {
            AccessToken accessToken = new AccessToken(
                    aliyun.getAccessKeyId(),
                    aliyun.getAccessKeySecret()
            );
            accessToken.apply();

            String token = accessToken.getToken();
            cachedToken.set(token);
            tokenExpireTime = accessToken.getExpireTime();

            String gatewayUrl = aliyun.getGatewayUrl();
            nlsClient = new NlsClient(gatewayUrl, token);

            log.info("NlsClient 初始化成功, gateway={}", gatewayUrl);
        } catch (Exception e) {
            log.error("NlsClient 初始化失败", e);
            throw new IllegalStateException("NlsClient 初始化失败: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (nlsClient != null) {
            nlsClient.shutdown();
            log.info("NlsClient 已关闭");
        }
    }
}

package com.mianshiba.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.speech")
public class SpeechProperties {

    private String provider = "aliyun";
    private Aliyun aliyun = new Aliyun();

    @Data
    public static class Aliyun {
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String appKey = "";
        private String region = "cn-shanghai";

        public String getGatewayUrl() {
            return "wss://nls-gateway-" + region + ".aliyuncs.com/ws/v1";
        }
    }
}

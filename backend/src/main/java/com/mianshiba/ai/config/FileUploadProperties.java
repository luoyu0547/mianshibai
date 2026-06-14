package com.mianshiba.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.file")
public class FileUploadProperties {

    /**
     * 文件存储提供方：local / aliyun
     */
    private String provider = "local";

    /**
     * 文件上传目录
     */
    private String uploadDir = "uploads";

    /**
     * 公开访问路径前缀
     */
    private String publicPrefix = "/uploads";

    /**
     * 单文件最大字节数
     */
    private long maxSize = 2 * 1024 * 1024;

    /**
     * 阿里云 OSS 配置
     */
    private Aliyun aliyun = new Aliyun();

    /**
     * 阿里云 OSS 配置项
     */
    @Data
    public static class Aliyun {

        /**
         * OSS Endpoint
         */
        private String endpoint = "";

        /**
         * OSS Bucket
         */
        private String bucket = "";

        /**
         * AccessKey ID
         */
        private String accessKeyId = "";

        /**
         * AccessKey Secret
         */
        private String accessKeySecret = "";

        /**
         * 自定义访问域名，可为空
         */
        private String domain = "";
    }
}

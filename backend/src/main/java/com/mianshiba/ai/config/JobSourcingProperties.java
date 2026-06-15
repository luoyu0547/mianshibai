package com.mianshiba.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 职位采集配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.job-sourcing")
public class JobSourcingProperties {

    /**
     * Playwright 浏览器 profile 根目录
     */
    private String browserProfileRoot = "backend/browser-profiles";

    /**
     * 是否以无头模式启动浏览器
     */
    private boolean browserHeadless = false;

    /**
     * 浏览器操作超时（毫秒）
     */
    private int browserTimeoutMillis = 30000;

    /**
     * 每个来源的期望采集职位数量
     */
    private int targetCount = 20;

    /**
     * 每个来源最大翻页数
     */
    private int maxPagesPerSource = 5;
}

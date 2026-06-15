package com.mianshiba.ai.model.dto.jobsourcing;

/**
 * 已抓取的职位详情页
 * 包含页面原始内容、最终 URL 及平台元信息。
 */
public record FetchedJobPage(
        /**
         * 源请求 URL
         */
        String sourceUrl,
        /**
         * 最终跳转后的 URL
         */
        String finalUrl,
        /**
         * 页面标题
         */
        String title,
        /**
         * 页面正文文本
         */
        String content,
        /**
         * 原始 HTML
         */
        String html,
        /**
         * 来源平台标识
         */
        String sourcePlatform,
        /**
         * 是否需要登录授权
         */
        boolean requiresAuth
) {
}

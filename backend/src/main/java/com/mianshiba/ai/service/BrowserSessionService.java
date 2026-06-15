package com.mianshiba.ai.service;

/**
 * 浏览器会话服务
 * 管理职位采集平台（如 Boss 直聘）的 Playwright 授权会话。
 */
public interface BrowserSessionService {

    AuthStartResult startAuth(String platform);

    AuthCheckResult checkAuth(String platform);

    record AuthStartResult(String platform, String profilePath) {
    }

    record AuthCheckResult(String platform, String status, String message) {
    }
}

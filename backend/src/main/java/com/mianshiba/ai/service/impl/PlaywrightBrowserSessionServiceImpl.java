package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.mapper.PlatformAuthSessionMapper;
import com.mianshiba.ai.model.entity.PlatformAuthSession;
import com.mianshiba.ai.model.enums.PlatformAuthStatus;
import com.mianshiba.ai.service.BrowserSessionService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Playwright 的浏览器会话服务实现
 * 管理平台授权 profile，并通过真实浏览器完成登录态检测。
 */
@Slf4j
@Service
public class PlaywrightBrowserSessionServiceImpl implements BrowserSessionService {

    private final PlatformAuthSessionMapper platformAuthSessionMapper;
    private final JobSourcingProperties properties;
    private final Map<String, ActiveAuthBrowser> activeAuthBrowsers = new ConcurrentHashMap<>();

    public PlaywrightBrowserSessionServiceImpl(PlatformAuthSessionMapper platformAuthSessionMapper,
                                               JobSourcingProperties properties) {
        this.platformAuthSessionMapper = platformAuthSessionMapper;
        this.properties = properties;
    }

    @Override
    public AuthStartResult startAuth(String platform) {
        String normalized = platform.toLowerCase();
        String profilePath = Path.of(properties.getBrowserProfileRoot(), normalized)
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace("\\", "/");

        QueryWrapper<PlatformAuthSession> wrapper = new QueryWrapper<>();
        wrapper.eq("platform", normalized);
        PlatformAuthSession session = platformAuthSessionMapper.selectOne(wrapper);

        if (session == null) {
            session = new PlatformAuthSession();
            session.setPlatform(normalized);
            session.setStatus("auth_required");
            session.setProfilePath(profilePath);
            session.setCreateTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());
            platformAuthSessionMapper.insert(session);
        } else {
            session.setStatus("auth_required");
            session.setProfilePath(profilePath);
            session.setUpdateTime(LocalDateTime.now());
            platformAuthSessionMapper.updateById(session);
        }

        openAuthBrowser(normalized, profilePath);

        return new AuthStartResult(normalized, profilePath);
    }

    @Override
    public AuthCheckResult checkAuth(String platform) {
        String normalized = platform.toLowerCase();
        PlatformAuthSession session = selectSession(normalized);

        if (session == null) {
            return new AuthCheckResult(normalized, "not_authorized", "未开始授权");
        }
        boolean authorized = isProfileAuthorized(normalized, session.getProfilePath());
        session.setStatus(authorized ? PlatformAuthStatus.AUTHORIZED.getValue() : PlatformAuthStatus.AUTH_REQUIRED.getValue());
        session.setLastVerifiedAt(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setErrorMessage(authorized ? "已授权" : "请在授权浏览器中完成登录");
        platformAuthSessionMapper.updateById(session);
        return new AuthCheckResult(normalized, session.getStatus(), session.getErrorMessage());
    }

    @Override
    public String getProfilePath(String platform) {
        String normalized = platform.toLowerCase();
        PlatformAuthSession session = selectSession(normalized);
        if (session != null && session.getProfilePath() != null && !session.getProfilePath().isBlank()) {
            return session.getProfilePath();
        }
        return Path.of(properties.getBrowserProfileRoot(), normalized)
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace("\\", "/");
    }

    private PlatformAuthSession selectSession(String platform) {
        QueryWrapper<PlatformAuthSession> wrapper = new QueryWrapper<>();
        wrapper.eq("platform", platform);
        return platformAuthSessionMapper.selectOne(wrapper);
    }

    /**
     * 打开平台授权浏览器
     *
     * @param platform    平台标识
     * @param profilePath profile 路径
     */
    protected void openAuthBrowser(String platform, String profilePath) {
        // 1. 关闭同平台旧授权窗口，避免 profile 被重复占用
        closeActiveBrowser(platform);

        // 2. 启动持久化 profile 的有头浏览器，供管理员手动登录
        Playwright playwright = Playwright.create();
        BrowserContext context = playwright.chromium().launchPersistentContext(Path.of(profilePath),
                new BrowserType.LaunchPersistentContextOptions()
                        .setHeadless(false)
                        .setTimeout(properties.getBrowserTimeoutMillis()));
        Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        page.setDefaultTimeout(properties.getBrowserTimeoutMillis());
        page.navigate(authEntryUrl(platform), new Page.NavigateOptions()
                .setTimeout(properties.getBrowserTimeoutMillis())
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        activeAuthBrowsers.put(platform, new ActiveAuthBrowser(playwright, context));
    }

    /**
     * 检查 profile 是否已完成授权
     *
     * @param platform    平台标识
     * @param profilePath profile 路径
     * @return true 表示已授权
     */
    protected boolean isProfileAuthorized(String platform, String profilePath) {
        ActiveAuthBrowser active = activeAuthBrowsers.get(platform);
        if (active != null) {
            return isContextAuthorized(platform, active.context());
        }

        // 1. 无活动授权窗口时，短暂打开 headless profile 检测登录态
        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(Path.of(profilePath),
                     new BrowserType.LaunchPersistentContextOptions()
                             .setHeadless(true)
                             .setTimeout(properties.getBrowserTimeoutMillis()))) {
            return isContextAuthorized(platform, context);
        } catch (RuntimeException e) {
            log.warn("检查平台授权状态失败: platform={}", platform, e);
            return false;
        }
    }

    @PreDestroy
    public void closeAllActiveBrowsers() {
        activeAuthBrowsers.keySet().forEach(this::closeActiveBrowser);
    }

    private boolean isContextAuthorized(String platform, BrowserContext context) {
        Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        page.setDefaultTimeout(properties.getBrowserTimeoutMillis());
        page.navigate(authCheckUrl(platform), new Page.NavigateOptions()
                .setTimeout(properties.getBrowserTimeoutMillis())
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        waitForPageQuietly(page);
        String currentUrl = page.url().toLowerCase();
        String html = page.content();
        return !currentUrl.contains("login") && !isAuthWall(html);
    }

    private boolean isAuthWall(String html) {
        if (html == null || html.isBlank()) {
            return true;
        }
        return html.contains("登录/注册")
                || html.contains("请先登录")
                || html.contains("验证码")
                || html.contains("安全验证");
    }

    private String authEntryUrl(String platform) {
        if ("boss".equals(platform)) {
            return "https://www.zhipin.com/";
        }
        return "https://www.shixiseng.com/";
    }

    private String authCheckUrl(String platform) {
        if ("boss".equals(platform)) {
            return "https://www.zhipin.com/web/geek/job?query=Java&city=101010100";
        }
        return "https://www.shixiseng.com/interns?keyword=Java&city=%E5%8C%97%E4%BA%AC&page=1";
    }

    private void waitForPageQuietly(Page page) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(Math.min(properties.getBrowserTimeoutMillis(), 10000)));
        } catch (RuntimeException e) {
            log.debug("授权检测页面网络空闲等待超时，继续使用当前 DOM", e);
        }
    }

    private void closeActiveBrowser(String platform) {
        ActiveAuthBrowser active = activeAuthBrowsers.remove(platform);
        if (active == null) {
            return;
        }
        try {
            active.context().close();
        } finally {
            active.playwright().close();
        }
    }

    private record ActiveAuthBrowser(Playwright playwright, BrowserContext context) {
    }
}

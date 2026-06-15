package com.mianshiba.ai.tools;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Boss 直聘授权 profile 打开工具
 * 用于在本地打开持久化浏览器 profile，让管理员手动登录并保存 Cookie。
 */
public final class OpenBossAuthProfile {

    private static final String DEFAULT_PROFILE_ROOT = "browser-profiles";
    private static final String BOSS_HOME_URL = "https://www.zhipin.com/";
    private static final int DEFAULT_KEEP_ALIVE_MINUTES = 30;
    private static final String DEFAULT_CHROME_PATH = "C:/Program Files/Google/Chrome/Application/chrome.exe";

    private OpenBossAuthProfile() {
    }

    public static void main(String[] args) {
        // 1. 解析 profile 根目录和保活时间参数
        String profileRoot = args.length > 0 && !args[0].isBlank() ? args[0] : DEFAULT_PROFILE_ROOT;
        int keepAliveMinutes = parseKeepAliveMinutes(args);
        Path profilePath = Path.of(profileRoot, "boss");

        System.out.println("Starting Playwright with profile: " + profilePath.toAbsolutePath());
        System.out.println("Using Chrome executable: " + chromeExecutablePath());

        // 2. 打开有头浏览器，使用与后端抓取一致的 Boss profile
        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(profilePath,
                     new BrowserType.LaunchPersistentContextOptions()
                             .setExecutablePath(chromeExecutablePath())
                             .setArgs(List.of("--disable-gpu", "--disable-software-rasterizer"))
                             .setHeadless(false)
                             .setTimeout(60_000))) {
            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
            page.setDefaultTimeout(60_000);
            page.navigate(BOSS_HOME_URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60_000));

            // 3. 保持浏览器打开，等待管理员手动完成登录；关闭终端或超时后 profile 会写盘
            System.out.println("Boss auth profile opened: " + profilePath.toAbsolutePath());
            System.out.println("Please login in the opened browser. This window will keep it alive for "
                    + keepAliveMinutes + " minutes.");
            System.out.println("After login succeeds, close this PowerShell window to stop the browser if needed.");
            Thread.sleep(Duration.ofMinutes(keepAliveMinutes).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static Path chromeExecutablePath() {
        String envPath = System.getenv("BOSS_AUTH_CHROME_PATH");
        if (envPath != null && !envPath.isBlank()) {
            return Path.of(envPath);
        }
        return Path.of(DEFAULT_CHROME_PATH);
    }

    private static int parseKeepAliveMinutes(String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            return DEFAULT_KEEP_ALIVE_MINUTES;
        }
        try {
            return Math.max(1, Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            return DEFAULT_KEEP_ALIVE_MINUTES;
        }
    }
}

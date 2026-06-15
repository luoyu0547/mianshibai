package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Boss 直聘平台适配器
 * 负责 Boss 直聘职位页面的发现、抓取与兜底解析。
 */
@Slf4j
@Service
public class BossJobPlatformAdapter implements JobPlatformAdapter {

    /**
     * 浏览器会话服务，用于检查平台授权状态
     */
    private final BrowserSessionService browserSessionService;

    /**
     * 职位采集配置
     */
    private final JobSourcingProperties properties;

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BossJobPlatformAdapter(BrowserSessionService browserSessionService) {
        this(browserSessionService, new JobSourcingProperties());
    }

    @Autowired
    public BossJobPlatformAdapter(BrowserSessionService browserSessionService, JobSourcingProperties properties) {
        this.browserSessionService = browserSessionService;
        this.properties = properties;
    }

    /**
     * 授权墙关键词
     */
    private static final List<String> AUTH_WALL_KEYWORDS = List.of("登录", "验证码", "安全验证");

    /**
     * 技术栈关键词（按长度降序，优先匹配多词）
     */
    private static final List<String> TECH_STACK_KEYWORDS = List.of(
            "Spring Boot", "Spring Cloud", "Spring", "MySQL", "Redis", "MongoDB",
            "Elasticsearch", "Kafka", "RabbitMQ", "RocketMQ", "Docker", "Kubernetes",
            "Linux", "Nginx", "MyBatis", "MyBatis-Plus", "JPA", "Hibernate",
            "Dubbo", "ZooKeeper", "Nacos", "Consul", "Eureka", "Feign",
            "Java", "Go", "Golang", "Python", "C++", "C#", "JavaScript", "TypeScript",
            "Vue", "Vue.js", "React", "React.js", "Angular", "Node.js", "Express",
            "HTML", "CSS", "Sass", "Less", "Webpack", "Vite"
    );

    /**
     * 职位标题选择器正则
     */
    private static final Pattern TITLE_PATTERN = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 公司名称选择器正则
     */
    private static final Pattern COMPANY_PATTERN = Pattern.compile("<div[^>]*class=\"company\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 城市选择器正则
     */
    private static final Pattern CITY_PATTERN = Pattern.compile("<span[^>]*class=\"location\"[^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 薪资选择器正则
     */
    private static final Pattern SALARY_PATTERN = Pattern.compile("<span[^>]*class=\"salary\"[^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 经验选择器正则
     */
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("<span[^>]*class=\"experience\"[^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 学历选择器正则
     */
    private static final Pattern DEGREE_PATTERN = Pattern.compile("<span[^>]*class=\"degree\"[^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 职位描述选择器正则
     */
    private static final Pattern JOB_DESC_PATTERN = Pattern.compile("<div[^>]*class=\"job-sec-text\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * HTML 标签清理正则
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * 搜索结果卡片块正则
     */
    private static final Pattern SEARCH_CARD_PATTERN = Pattern.compile("(?is)<div[^>]*class=\"[^\"]*job-card-wrapper[^\"]*\"[^>]*>(.*?)(?=<div[^>]*class=\"[^\"]*job-card-wrapper|</body>|</html>)");

    /**
     * href 属性正则
     */
    private static final Pattern HREF_PATTERN = Pattern.compile("(?is)<a[^>]*href=\"([^\"]+)\"[^>]*>");

    /**
     * 搜索卡片字段正则
     */
    private static final Pattern SEARCH_TITLE_PATTERN = Pattern.compile("(?is)<span[^>]*class=\"[^\"]*job-name[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern SEARCH_CITY_PATTERN = Pattern.compile("(?is)<span[^>]*class=\"[^\"]*job-area[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern SEARCH_COMPANY_PATTERN = Pattern.compile("(?is)<div[^>]*class=\"[^\"]*company-name[^\"]*\"[^>]*>(.*?)</div>");

    @Override
    public String platform() {
        return "boss";
    }

    @Override
    public boolean requiresAuth() {
        return true;
    }

    @Override
    public BrowserSessionService.AuthCheckResult checkAuth() {
        // 1. 委托浏览器会话服务检查 boss 平台授权状态
        return browserSessionService.checkAuth("boss");
    }

    @Override
    public List<JobListEntry> discover(JobDiscoveryRequest request) {
        try {
            // 1. 逐页加载 Boss 搜索结果页
            Map<String, JobListEntry> entries = new LinkedHashMap<>();
            int maxPages = Math.max(1, request.maxPages());
            int targetCount = Math.max(1, request.targetCount());
            for (int page = 1; page <= maxPages && entries.size() < targetCount; page++) {
                String url = buildSearchUrl(request, page);
                String html = loadPageHtml(url);
                if (isAuthWall(html)) {
                    throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "Boss 授权失效");
                }
                for (JobListEntry entry : parseSearchEntries(html)) {
                    entries.putIfAbsent(entry.sourceUrl(), entry);
                    if (entries.size() >= targetCount) {
                        break;
                    }
                }
            }
            if (entries.isEmpty()) {
                throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "Boss 未发现职位，可能未授权或页面结构变化");
            }
            return new ArrayList<>(entries.values());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Boss discover failed, request={}", request, e);
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR);
        }
    }

    @Override
    public FetchedJobPage fetchDetail(String url) {
        try {
            // 1. 通过授权 profile 加载职位详情页
            String html = loadPageHtml(url);
            if (isAuthWall(html)) {
                throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "Boss 授权失效");
            }

            // 2. 组装可供后续兜底抽取的页面对象
            return new FetchedJobPage(url, url, extractTitle(html), stripHtml(html), html, platform(), true);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Boss fetchDetail failed, url={}", url, e);
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR);
        }
    }

    /**
     * 加载页面 HTML
     *
     * @param url 目标 URL
     * @return 页面 HTML
     */
    protected String loadPageHtml(String url) throws IOException {
        // 1. 使用平台专属 profile 启动持久化上下文，复用管理员登录态
        Path profilePath = Path.of(browserSessionService.getProfilePath(platform()));
        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(profilePath,
                     new BrowserType.LaunchPersistentContextOptions()
                             .setExecutablePath(chromeExecutablePath())
                             .setArgs(List.of("--disable-blink-features=AutomationControlled"))
                             .setHeadless(properties.isBrowserHeadless())
                             .setTimeout(properties.getBrowserTimeoutMillis()))) {
            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
            page.setDefaultTimeout(properties.getBrowserTimeoutMillis());
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(properties.getBrowserTimeoutMillis())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            waitForPageQuietly(page);
            return page.content();
        }
    }

    private String buildSearchUrl(JobDiscoveryRequest request, int page) {
        String keyword = encode(request.keywords());
        String cityCode = bossCityCode(request.cities());
        return "https://www.zhipin.com/web/geek/job?query=" + keyword + "&city=" + cityCode + "&page=" + page;
    }

    private List<JobListEntry> parseSearchEntries(String html) {
        List<JobListEntry> entries = new ArrayList<>();
        Matcher matcher = SEARCH_CARD_PATTERN.matcher(html);
        while (matcher.find()) {
            String cardHtml = matcher.group(1);
            String href = extractFirst(HREF_PATTERN, cardHtml);
            String title = extractFirst(SEARCH_TITLE_PATTERN, cardHtml);
            if (href == null || title == null) {
                continue;
            }
            entries.add(new JobListEntry(
                    platform(),
                    normalizeUrl(href),
                    title,
                    extractFirst(SEARCH_COMPANY_PATTERN, cardHtml),
                    extractFirst(SEARCH_CITY_PATTERN, cardHtml),
                    extractFirst(SALARY_PATTERN, cardHtml)
            ));
        }
        return entries;
    }

    private String normalizeUrl(String href) {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (href.startsWith("/")) {
            return "https://www.zhipin.com" + href;
        }
        return "https://www.zhipin.com/" + href;
    }

    private String bossCityCode(String cities) {
        if (cities == null || cities.isBlank()) {
            return "100010000";
        }
        if (cities.contains("北京")) {
            return "101010100";
        }
        if (cities.contains("上海")) {
            return "101020100";
        }
        if (cities.contains("广州")) {
            return "101280100";
        }
        if (cities.contains("深圳")) {
            return "101280600";
        }
        if (cities.contains("杭州")) {
            return "101210100";
        }
        if (cities.contains("成都")) {
            return "101270100";
        }
        return "100010000";
    }

    private Path chromeExecutablePath() {
        String envPath = System.getenv("BOSS_AUTH_CHROME_PATH");
        if (envPath != null && !envPath.isBlank()) {
            return Path.of(envPath);
        }
        Path chrome = Path.of("C:/Program Files/Google/Chrome/Application/chrome.exe");
        if (java.nio.file.Files.exists(chrome)) {
            return chrome;
        }
        Path edge = Path.of("C:/Program Files/Microsoft/Edge/Application/msedge.exe");
        if (java.nio.file.Files.exists(edge)) {
            return edge;
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void waitForPageQuietly(Page page) {
        try {
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(Math.min(properties.getBrowserTimeoutMillis(), 10000)));
        } catch (RuntimeException e) {
            log.debug("Boss 页面网络空闲等待超时，继续使用当前 DOM", e);
        }
    }

    private String extractTitle(String html) {
        String title = extractFirst(Pattern.compile("(?is)<title[^>]*>(.*?)</title>"), html);
        return title == null ? "" : title;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    @Override
    public ExtractedJobCard fallbackExtract(FetchedJobPage page) {
        // 1. 检查是否命中授权墙
        if (isAuthWall(page.html())) {
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "Boss 授权失效");
        }

        // 2. 优先使用正文文本，否则回退到 HTML
        String text = page.html() != null && !page.html().isBlank() ? page.html() : page.content();
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "Boss 页面内容为空");
        }

        // 3. 抽取基础字段
        String title = firstNonBlank(extractFirst(TITLE_PATTERN, text), page.title());
        String companyName = extractFirst(COMPANY_PATTERN, text);
        String city = extractFirst(CITY_PATTERN, text);
        String salaryRange = extractFirst(SALARY_PATTERN, text);
        String experienceRequirement = extractFirst(EXPERIENCE_PATTERN, text);
        String educationRequirement = extractFirst(DEGREE_PATTERN, text);
        String jobDescription = extractFirst(JOB_DESC_PATTERN, text);

        // 4. 从职位描述中检测技术栈标签
        List<String> techStack = extractTechStack(jobDescription != null ? jobDescription : text);

        // 5. 构建并返回提取的职位卡片
        return new ExtractedJobCard(
                title,
                companyName,
                city,
                salaryRange,
                experienceRequirement,
                educationRequirement,
                jobDescription,
                null,
                toJson(techStack),
                null,
                null,
                80
        );
    }

    /**
     * 判断页面是否为授权墙
     *
     * @param html 页面 HTML
     * @return true 表示命中授权墙
     */
    boolean isAuthWall(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        // 1. 页面已包含可识别职位列表或详情字段时，隐藏登录弹窗文案不能作为授权墙依据
        if (SEARCH_CARD_PATTERN.matcher(html).find()
                || SEARCH_TITLE_PATTERN.matcher(html).find()
                || TITLE_PATTERN.matcher(html).find()) {
            return false;
        }

        // 2. 没有职位内容时，再检查是否包含授权墙关键词
        for (String keyword : AUTH_WALL_KEYWORDS) {
            if (html.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 使用正则从文本中抽取第一个匹配组
     *
     * @param pattern 正则模式
     * @param text    源文本
     * @return 匹配内容，未命中返回 null
     */
    private String extractFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return stripHtml(matcher.group(1));
        }
        return null;
    }

    /**
     * 清理 HTML 标签并压缩空白
     *
     * @param html 原始 HTML 片段
     * @return 纯文本
     */
    private String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        // 1. 移除 HTML 标签
        String text = HTML_TAG_PATTERN.matcher(html).replaceAll("");
        // 2. 解码常见 HTML 实体
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        // 3. 压缩空白字符
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * 从文本中检测技术栈关键词
     *
     * @param text 源文本
     * @return 检测到的技术栈列表
     */
    private List<String> extractTechStack(String text) {
        Set<String> result = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return new ArrayList<>(result);
        }
        // 1. 遍历关键词列表，按顺序检测
        for (String keyword : TECH_STACK_KEYWORDS) {
            if (text.contains(keyword)) {
                // 统一部分别名
                String normalized = keyword;
                if ("Golang".equals(keyword)) {
                    normalized = "Go";
                } else if ("Vue.js".equals(keyword)) {
                    normalized = "Vue";
                } else if ("React.js".equals(keyword)) {
                    normalized = "React";
                }
                result.add(normalized);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 将列表序列化为 JSON 字符串
     *
     * @param list 列表数据
     * @return JSON 字符串
     */
    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("序列化技术栈失败", e);
            return "[]";
        }
    }
}

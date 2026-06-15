package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
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
import com.mianshiba.ai.model.enums.PlatformAuthStatus;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实习僧平台适配器
 * 负责实习僧职位页面的发现、抓取与兜底解析。
 */
@Slf4j
@Service
public class ShixisengJobPlatformAdapter implements JobPlatformAdapter {

    /**
     * 职位采集配置
     */
    private final JobSourcingProperties properties;

    public ShixisengJobPlatformAdapter() {
        this(new JobSourcingProperties());
    }

    @Autowired
    public ShixisengJobPlatformAdapter(JobSourcingProperties properties) {
        this.properties = properties;
    }

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 职位标题选择器正则
     */
    private static final Pattern TITLE_PATTERN = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 真实详情页标题正则
     */
    private static final Pattern NEW_JOB_NAME_PATTERN = Pattern.compile("(?is)<div[^>]*class=\"[^\"]*new_job_name[^\"]*\"[^>]*>(.*?)</div>");

    /**
     * 公司名称选择器正则
     */
    private static final Pattern COMPANY_PATTERN = Pattern.compile("<div[^>]*class=\"com-name\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 真实详情页公司名称正则
     */
    private static final Pattern COMPANY_ANCHOR_PATTERN = Pattern.compile("(?is)<a[^>]*class=\"[^\"]*com-name[^\"]*\"[^>]*>(.*?)</a>");

    /**
     * 职位描述选择器正则
     */
    private static final Pattern JOB_DESC_PATTERN = Pattern.compile("<div[^>]*class=\"job_detail\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * span 标签选择器正则
     */
    private static final Pattern SPAN_PATTERN = Pattern.compile("<span[^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * HTML 标签清理正则
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * 实习薪资匹配正则
     */
    private static final Pattern SALARY_PATTERN = Pattern.compile("\\d+[-~]\\d+/天");

    /**
     * 学历要求匹配正则
     */
    private static final Pattern EDUCATION_PATTERN = Pattern.compile("本科|硕士|大专|博士|不限");

    /**
     * 实习标签匹配正则（每周 X 天、实习 X 个月）
     */
    private static final Pattern TAG_PATTERN = Pattern.compile("每周\\s*\\d+\\s*天|实习\\s*\\d+\\s*个月");

    /**
     * 搜索结果卡片块正则
     */
    private static final Pattern SEARCH_CARD_PATTERN = Pattern.compile("(?is)<div[^>]*class=\"[^\"]*(?:intern-wrap|intern-item)[^\"]*\"[^>]*>(.*?)(?=<div[^>]*class=\"[^\"]*(?:intern-wrap|intern-item)|</body>|</html>)");

    /**
     * 搜索卡片字段正则
     */
    private static final Pattern HREF_PATTERN = Pattern.compile("(?is)<a[^>]*href=\"([^\"]+)\"[^>]*>");
    private static final Pattern SEARCH_TITLE_PATTERN = Pattern.compile("(?is)<a[^>]*class=\"[^\"]*title[^\"]*\"[^>]*>(.*?)</a>");
    private static final Pattern SEARCH_COMPANY_PATTERN = Pattern.compile("(?is)<a[^>]*class=\"[^\"]*company-name[^\"]*\"[^>]*>(.*?)</a>");
    private static final Pattern SEARCH_COMPANY_ALT_PATTERN = Pattern.compile("(?is)<div[^>]*class=\"[^\"]*intern-detail__company[^\"]*\"[^>]*>.*?<a[^>]*title=\"([^\"]+)\"");
    private static final Pattern SEARCH_SALARY_PATTERN = Pattern.compile("(?is)<span[^>]*class=\"[^\"]*(?:day|salary)[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern SEARCH_CITY_PATTERN = Pattern.compile("(?is)<span[^>]*class=\"[^\"]*city[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern DETAIL_CITY_PATTERN = Pattern.compile("(?is)<span[^>]*class=\"[^\"]*job_position[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern DETAIL_SALARY_PATTERN = Pattern.compile("(?is)<span[^>]*class=\"[^\"]*job_money[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern DETAIL_EDUCATION_PATTERN = Pattern.compile("(?is)<span[^>]*class=\"[^\"]*job_academic[^\"]*\"[^>]*>(.*?)</span>");

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

    @Override
    public String platform() {
        return "shixiseng";
    }

    @Override
    public boolean requiresAuth() {
        return false;
    }

    @Override
    public BrowserSessionService.AuthCheckResult checkAuth() {
        // 1. 实习僧为公开访问，直接返回已授权状态
        return new BrowserSessionService.AuthCheckResult("shixiseng", PlatformAuthStatus.AUTHORIZED.getValue(), "公开访问");
    }

    @Override
    public List<JobListEntry> discover(JobDiscoveryRequest request) {
        try {
            // 1. 逐页加载实习僧搜索结果页
            Map<String, JobListEntry> entries = new LinkedHashMap<>();
            int maxPages = Math.max(1, request.maxPages());
            int targetCount = Math.max(1, request.targetCount());
            for (int page = 1; page <= maxPages && entries.size() < targetCount; page++) {
                String url = buildSearchUrl(request, page);
                String html = loadPageHtml(url);
                for (JobListEntry entry : parseSearchEntries(html)) {
                    entries.putIfAbsent(entry.sourceUrl(), entry);
                    if (entries.size() >= targetCount) {
                        break;
                    }
                }
            }
            return new ArrayList<>(entries.values());
        } catch (Exception e) {
            log.error("Shixiseng discover failed, request={}", request, e);
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR);
        }
    }

    @Override
    public FetchedJobPage fetchDetail(String url) {
        try {
            // 1. 加载公开职位详情页
            String html = loadPageHtml(url);

            // 2. 组装可供后续兜底抽取的页面对象
            return new FetchedJobPage(url, url, extractTitle(html), stripHtml(html), html, platform(), false);
        } catch (Exception e) {
            log.error("Shixiseng fetchDetail failed, url={}", url, e);
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
        // 1. 使用无状态浏览器上下文抓取公开页面
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                     .setHeadless(properties.isBrowserHeadless())
                     .setTimeout(properties.getBrowserTimeoutMillis()));
             BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
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
        String city = encode(firstToken(request.cities()));
        return "https://www.shixiseng.com/interns?keyword=" + keyword + "&city=" + city + "&page=" + page;
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
                    firstNonBlank(extractFirst(SEARCH_COMPANY_PATTERN, cardHtml), extractFirst(SEARCH_COMPANY_ALT_PATTERN, cardHtml)),
                    extractFirst(SEARCH_CITY_PATTERN, cardHtml),
                    extractFirst(SEARCH_SALARY_PATTERN, cardHtml)
            ));
        }
        return entries;
    }

    private String normalizeUrl(String href) {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (href.startsWith("/")) {
            return "https://www.shixiseng.com" + href;
        }
        return "https://www.shixiseng.com/" + href;
    }

    private String firstToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.split("[,，;；/\\s]+", 2)[0];
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void waitForPageQuietly(Page page) {
        try {
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(Math.min(properties.getBrowserTimeoutMillis(), 10000)));
        } catch (RuntimeException e) {
            log.debug("实习僧页面网络空闲等待超时，继续使用当前 DOM", e);
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
        // 1. 优先使用正文文本，否则回退到 HTML
        String text = page.html() != null && !page.html().isBlank() ? page.html() : page.content();
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "实习僧页面内容为空");
        }

        // 2. 抽取基础字段
        String title = firstNonBlank(firstNonBlank(extractFirst(TITLE_PATTERN, text), extractFirst(NEW_JOB_NAME_PATTERN, text)), page.title());
        String companyName = firstNonBlank(extractFirst(COMPANY_PATTERN, text), extractFirst(COMPANY_ANCHOR_PATTERN, text));
        String jobDescription = extractFirst(JOB_DESC_PATTERN, text);

        // 3. 解析所有 span 标签，分类提取城市、薪资、学历、标签
        List<String> spans = extractAll(SPAN_PATTERN, text);
        String city = extractFirst(DETAIL_CITY_PATTERN, text);
        String salaryRange = extractFirst(DETAIL_SALARY_PATTERN, text);
        String educationRequirement = extractFirst(DETAIL_EDUCATION_PATTERN, text);
        List<String> tags = new ArrayList<>();
        for (String span : spans) {
            if (salaryRange == null && SALARY_PATTERN.matcher(span).matches()) {
                salaryRange = span;
            } else if (educationRequirement == null && EDUCATION_PATTERN.matcher(span).matches()) {
                educationRequirement = span;
            } else if (TAG_PATTERN.matcher(span).matches()) {
                tags.add(span);
            } else if (city == null && !span.isBlank()) {
                city = span;
            }
        }

        // 4. 从职位描述中检测技术栈标签
        List<String> techStack = extractTechStack(jobDescription != null ? jobDescription : text);

        // 5. 构建并返回提取的职位卡片
        return new ExtractedJobCard(
                title,
                companyName,
                city,
                salaryRange,
                null,
                educationRequirement,
                jobDescription,
                null,
                toJson(techStack),
                null,
                toJson(tags),
                80
        );
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
     * 使用正则从文本中抽取所有匹配组
     *
     * @param pattern 正则模式
     * @param text    源文本
     * @return 匹配内容列表
     */
    private List<String> extractAll(Pattern pattern, String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(stripHtml(matcher.group(1)));
        }
        return result;
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
            log.error("序列化失败", e);
            return "[]";
        }
    }
}

package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Boss 直聘平台适配器
 * 负责 Boss 直聘职位页面的发现、抓取与兜底解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BossJobPlatformAdapter implements JobPlatformAdapter {

    /**
     * 浏览器会话服务，用于检查平台授权状态
     */
    private final BrowserSessionService browserSessionService;

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // 1. 列表页发现暂未实现，返回空列表
        log.info("Boss discover not implemented yet, request={}", request);
        return List.of();
    }

    @Override
    public FetchedJobPage fetchDetail(String url) {
        // 1. 真实详情页抓取暂未实现，抛出抓取异常
        log.warn("Boss fetchDetail not implemented yet, url={}", url);
        throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR);
    }

    @Override
    public ExtractedJobCard fallbackExtract(FetchedJobPage page) {
        // 1. 检查是否命中授权墙
        if (isAuthWall(page.html())) {
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "Boss 授权失效");
        }

        // 2. 优先使用正文文本，否则回退到 HTML
        String text = page.content() != null && !page.content().isBlank() ? page.content() : page.html();
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "Boss 页面内容为空");
        }

        // 3. 抽取基础字段
        String title = extractFirst(TITLE_PATTERN, text);
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
        // 1. 检查是否包含授权墙关键词
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

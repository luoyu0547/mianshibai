package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 职位标题选择器正则
     */
    private static final Pattern TITLE_PATTERN = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * 公司名称选择器正则
     */
    private static final Pattern COMPANY_PATTERN = Pattern.compile("<div[^>]*class=\"com-name\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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
        // 1. 列表页发现暂未实现，返回空列表
        log.info("Shixiseng discover not implemented yet, request={}", request);
        return List.of();
    }

    @Override
    public FetchedJobPage fetchDetail(String url) {
        // 1. 真实详情页抓取暂未实现，抛出抓取异常
        log.warn("Shixiseng fetchDetail not implemented yet, url={}", url);
        throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR);
    }

    @Override
    public ExtractedJobCard fallbackExtract(FetchedJobPage page) {
        // 1. 优先使用正文文本，否则回退到 HTML
        String text = page.content() != null && !page.content().isBlank() ? page.content() : page.html();
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.JOB_CRAWL_ERROR, "实习僧页面内容为空");
        }

        // 2. 抽取基础字段
        String title = extractFirst(TITLE_PATTERN, text);
        String companyName = extractFirst(COMPANY_PATTERN, text);
        String jobDescription = extractFirst(JOB_DESC_PATTERN, text);

        // 3. 解析所有 span 标签，分类提取城市、薪资、学历、标签
        List<String> spans = extractAll(SPAN_PATTERN, text);
        String city = null;
        String salaryRange = null;
        String educationRequirement = null;
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

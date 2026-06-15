package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.JobPageFetchService;
import com.mianshiba.ai.service.JobSourcingExtractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobSourcingExtractServiceImpl implements JobSourcingExtractService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Pattern GREENHOUSE_TITLE_PATTERN = Pattern.compile("(?i)^Job Application for\\s+(.+?)\\s+at\\s+(.+)$");
    private static final Pattern SALARY_PATTERN = Pattern.compile("\\$[\\d,]+\\s*-\\s*\\$[\\d,]+\\s*USD");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("\\b\\d+\\+?\\s*\\+?\\s*years?[^.。；;]*", Pattern.CASE_INSENSITIVE);
    private static final String PROMPT = "你是程序员岗位采集审核助手。请从网页内容提取待审核职位卡片，输出严格 JSON："
            + "{\"title\":\"\",\"companyName\":\"\",\"city\":\"\",\"salaryRange\":\"\","
            + "\"experienceRequirement\":\"\",\"educationRequirement\":\"\",\"jobDescription\":\"\","
            + "\"jobRequirement\":\"\",\"techStack\":[],\"summary\":\"\",\"tags\":[],\"confidenceScore\":0}";

    private final ChatClient chatClient;

    @Override
    public ExtractedJobCard extract(JobPageFetchService.FetchedPage page) {
        try {
            String response = chatClient.prompt().system(PROMPT).user(page.content()).call().content();
            if (response == null || response.isBlank()) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
            }
            JsonNode node = OBJECT_MAPPER.readTree(extractJson(response));
            return new ExtractedJobCard(
                    text(node, "title"), text(node, "companyName"), text(node, "city"),
                    text(node, "salaryRange"), text(node, "experienceRequirement"), text(node, "educationRequirement"),
                    text(node, "jobDescription"), text(node, "jobRequirement"),
                    node.path("techStack").toString(), text(node, "summary"),
                    node.path("tags").toString(), node.path("confidenceScore").asInt(0));
        } catch (Exception e) {
            log.warn("职位采集 AI 抽取失败，启用规则兜底: {}", page.sourceUrl(), e);
            return fallbackExtract(page);
        }
    }

    private ExtractedJobCard fallbackExtract(JobPageFetchService.FetchedPage page) {
        String title = page.title();
        String companyName = "";
        Matcher titleMatcher = GREENHOUSE_TITLE_PATTERN.matcher(page.title());
        if (titleMatcher.find()) {
            title = titleMatcher.group(1).trim();
            companyName = titleMatcher.group(2).trim();
        }

        String content = page.content() == null ? "" : page.content();
        String city = firstPresent(content, List.of("United States (remote)", "Remote", "远程"));
        String salaryRange = matchFirst(SALARY_PATTERN, content);
        String experienceRequirement = matchFirst(EXPERIENCE_PATTERN, content);
        String jobDescription = between(content, "What you will do", "What you will bring along");
        String jobRequirement = between(content, "What you will bring along", "Nice to have");
        List<String> techStack = detectTechStack(content);

        if (!StringUtils.hasText(title) || !StringUtils.hasText(companyName)) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        return new ExtractedJobCard(
                title,
                companyName,
                city,
                salaryRange,
                experienceRequirement,
                "",
                jobDescription,
                jobRequirement,
                toJsonArray(techStack),
                title + " at " + companyName,
                toJsonArray(List.of("自动采集", page.sourcePlatform())),
                65);
    }

    private String extractJson(String text) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : text.trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String matchFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : "";
    }

    private String firstPresent(String text, List<String> candidates) {
        return candidates.stream().filter(text::contains).findFirst().orElse("");
    }

    private String between(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex < 0) {
            return "";
        }
        int contentStart = startIndex + start.length();
        int endIndex = text.indexOf(end, contentStart);
        String value = endIndex > contentStart ? text.substring(contentStart, endIndex) : text.substring(contentStart);
        value = value.replaceAll("\\s+", " ").trim();
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private List<String> detectTechStack(String content) {
        List<String> techStack = new ArrayList<>();
        for (String keyword : List.of("Java", "Spring", "Python", "Go", "TypeScript", "AI/ML", "MySQL", "Redis", "AWS", "Azure", "GCP")) {
            if (content.contains(keyword)) {
                techStack.add(keyword);
            }
        }
        return techStack;
    }

    private String toJsonArray(List<String> values) {
        try {
            return OBJECT_MAPPER.writeValueAsString(values);
        } catch (Exception e) {
            return "[]";
        }
    }
}

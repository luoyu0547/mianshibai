package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.service.JobCrawlService.CrawlResult;
import com.mianshiba.ai.service.JobParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobParseServiceImpl implements JobParseService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern JSON_CODE_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

    private static final String JOB_PARSE_PROMPT =
            "你是一个职位信息解析专家。从以下网页内容中提取职位信息，输出严格的 JSON 格式。\n" +
            "如果某字段无法从内容中提取，使用空字符串。\n" +
            "JSON 格式如下：\n" +
            "{\"title\":\"\",\"companyName\":\"\",\"city\":\"\",\"salaryRange\":\"\",\"experienceRequirement\":\"\",\"educationRequirement\":\"\",\"jobDescription\":\"\",\"jobRequirement\":\"\",\"techStack\":[]}\n" +
            "只输出 JSON，不要输出任何其他内容。";

    private static final String COMPANY_PARSE_PROMPT =
            "你是一个公司信息解析专家。从以下网页内容中提取公司信息，输出严格的 JSON 格式。\n" +
            "如果某字段无法从内容中提取，使用空字符串。\n" +
            "JSON 格式如下：\n" +
            "{\"name\":\"\",\"industry\":\"\",\"city\":\"\",\"scale\":\"\",\"description\":\"\",\"mainBusiness\":\"\",\"techDirection\":\"\",\"website\":\"\"}\n" +
            "只输出 JSON，不要输出任何其他内容。";

    private final ChatClient chatClient;

    @Override
    public Job parseJob(CrawlResult crawlResult) {
        String aiResponse = callAi(JOB_PARSE_PROMPT, crawlResult.content());
        String json = extractJsonFromResponse(aiResponse);

        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("解析职位 JSON 失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.JOB_PARSE_ERROR);
        }

        Job job = new Job();
        job.setTitle(getTextOrEmpty(node, "title"));
        job.setCompanyName(getTextOrEmpty(node, "companyName"));
        job.setCity(getTextOrEmpty(node, "city"));
        job.setSalaryRange(getTextOrEmpty(node, "salaryRange"));
        job.setExperienceRequirement(getTextOrEmpty(node, "experienceRequirement"));
        job.setEducationRequirement(getTextOrEmpty(node, "educationRequirement"));
        job.setJobDescription(getTextOrEmpty(node, "jobDescription"));
        job.setJobRequirement(getTextOrEmpty(node, "jobRequirement"));
        job.setSourcePlatform(crawlResult.sourcePlatform());
        job.setSourceUrl(crawlResult.url());
        job.setRawContent(crawlResult.content());

        try {
            JsonNode techStackNode = node.get("techStack");
            if (techStackNode != null && techStackNode.isArray()) {
                job.setTechStack(objectMapper.writeValueAsString(techStackNode));
            } else {
                job.setTechStack("[]");
            }
        } catch (Exception e) {
            job.setTechStack("[]");
        }

        return job;
    }

    @Override
    public Company parseCompany(CrawlResult crawlResult) {
        String aiResponse = callAi(COMPANY_PARSE_PROMPT, crawlResult.content());
        String json = extractJsonFromResponse(aiResponse);

        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("解析公司 JSON 失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.JOB_PARSE_ERROR);
        }

        Company company = new Company();
        company.setName(getTextOrEmpty(node, "name"));
        company.setIndustry(getTextOrEmpty(node, "industry"));
        company.setCity(getTextOrEmpty(node, "city"));
        company.setScale(getTextOrEmpty(node, "scale"));
        company.setDescription(getTextOrEmpty(node, "description"));
        company.setMainBusiness(getTextOrEmpty(node, "mainBusiness"));
        company.setTechDirection(getTextOrEmpty(node, "techDirection"));
        company.setWebsite(getTextOrEmpty(node, "website"));
        company.setSourceUrl(crawlResult.url());

        return company;
    }

    private String callAi(String systemPrompt, String userMessage) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 服务调用失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private String extractJsonFromResponse(String text) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text.trim();
    }

    private String getTextOrEmpty(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }
}

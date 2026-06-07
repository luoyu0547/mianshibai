package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.service.AiJobAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobAnalysisServiceImpl implements AiJobAnalysisService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern JSON_CODE_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

    private static final String ANALYSIS_PROMPT =
            "你是一个技术岗位分析专家。分析以下职位信息，从技术面试官和 HR 双重视角输出分析结果。\n" +
            "输出严格的 JSON 格式：\n" +
            "{\"requirementSummary\":\"\",\"coreSkills\":[],\"hiddenRequirements\":[],\"interviewFocus\":[],\"resumeSuggestions\":[],\"riskPoints\":[]}\n" +
            "requirementSummary: 50-150字总结核心要求\n" +
            "coreSkills: 核心技术栈列表\n" +
            "hiddenRequirements: JD 中隐含但未明说的能力要求\n" +
            "interviewFocus: 面试应重点准备的方向\n" +
            "resumeSuggestions: 简历应如何针对此岗位优化的建议\n" +
            "riskPoints: 候选人可能的风险点或不匹配点\n" +
            "只输出 JSON，不要输出任何其他内容。";

    private final ChatClient chatClient;

    @Override
    public JobAnalysis analyzeJob(Job job) {
        String jobContent = buildJobContent(job);
        String aiResponse = callAi(ANALYSIS_PROMPT, jobContent);
        String json = extractJsonFromResponse(aiResponse);

        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("解析职位分析 JSON 失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.JOB_PARSE_ERROR);
        }

        JobAnalysis analysis = new JobAnalysis();
        analysis.setJobId(job.getId());
        analysis.setRequirementSummary(getTextOrEmpty(node, "requirementSummary"));
        analysis.setCoreSkills(toJsonString(node.get("coreSkills")));
        analysis.setHiddenRequirements(toJsonString(node.get("hiddenRequirements")));
        analysis.setInterviewFocus(toJsonString(node.get("interviewFocus")));
        analysis.setResumeSuggestions(toJsonString(node.get("resumeSuggestions")));
        analysis.setRiskPoints(toJsonString(node.get("riskPoints")));

        return analysis;
    }

    private String buildJobContent(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("职位名称：").append(nullToEmpty(job.getTitle())).append("\n");
        sb.append("公司：").append(nullToEmpty(job.getCompanyName())).append("\n");
        sb.append("城市：").append(nullToEmpty(job.getCity())).append("\n");
        sb.append("薪资范围：").append(nullToEmpty(job.getSalaryRange())).append("\n");
        sb.append("经验要求：").append(nullToEmpty(job.getExperienceRequirement())).append("\n");
        sb.append("学历要求：").append(nullToEmpty(job.getEducationRequirement())).append("\n");
        sb.append("职位描述：").append(nullToEmpty(job.getJobDescription())).append("\n");
        sb.append("职位要求：").append(nullToEmpty(job.getJobRequirement())).append("\n");
        sb.append("技术栈：").append(nullToEmpty(job.getTechStack()));
        return sb.toString();
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

    private String toJsonString(JsonNode node) {
        if (node == null || node.isNull()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}

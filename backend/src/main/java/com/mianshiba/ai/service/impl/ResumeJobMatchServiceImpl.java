package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.JobMatchMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.entity.JobMatch;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.service.JobRecommendService;
import com.mianshiba.ai.service.ResumeJobMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeJobMatchServiceImpl implements ResumeJobMatchService {

    private static final String SYSTEM_PROMPT = """
            你是一个技术岗位匹配专家。根据候选人的简历和目标岗位要求，输出匹配分析。
            输出严格的 JSON 格式：
            {"matchScore":0,"growthScore":0,"techGrowthScore":0,"salaryCityScore":0,"experienceFitScore":0,"recommendation":"","reason":"","gaps":[]}
            matchScore: 岗位匹配度 0-100
            growthScore: 企业成长性 0-100（考虑专精特新、行业前景等）
            techGrowthScore: 技术成长价值 0-100
            salaryCityScore: 薪资城市匹配 0-100
            experienceFitScore: 经验门槛适配 0-100
            recommendation: recommended/cautious/stretch/not_recommended
            reason: 推荐原因，50-200字
            gaps: 能力缺口列表
            只输出 JSON，不要输出任何其他内容。
            """;

    private final ChatClient chatClient;
    private final ResumeSectionMapper resumeSectionMapper;
    private final JobMapper jobMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final JobMatchMapper jobMatchMapper;
    private final JobRecommendService jobRecommendService;
    private final ObjectMapper objectMapper;

    @Override
    public JobMatch match(Long userId, Long resumeId, Long jobId) {
        List<ResumeSection> sections = resumeSectionMapper.selectList(
                Wrappers.lambdaQuery(ResumeSection.class)
                        .eq(ResumeSection::getResumeId, resumeId)
                        .orderByAsc(ResumeSection::getSortOrder));

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
        }

        JobAnalysis analysis = jobAnalysisMapper.selectOne(
                Wrappers.lambdaQuery(JobAnalysis.class)
                        .eq(JobAnalysis::getJobId, jobId)
                        .last("LIMIT 1"));

        String resumeData = sections.stream()
                .map(s -> s.getSectionType() + ": " + s.getSectionData())
                .collect(Collectors.joining("\n"));

        String analysisData = analysis != null
                ? "需求摘要: " + analysis.getRequirementSummary()
                + "\n核心技能: " + analysis.getCoreSkills()
                + "\n隐性要求: " + analysis.getHiddenRequirements()
                : "";

        String userMessage = """
                候选人简历数据：
                %s

                目标职位信息：
                职位名称: %s
                公司: %s
                城市: %s
                薪资范围: %s
                经验要求: %s
                学历要求: %s
                职位描述: %s
                职位要求: %s
                技术栈: %s

                AI 分析结果：
                %s
                """.formatted(resumeData, job.getTitle(), job.getCompanyName(),
                job.getCity(), job.getSalaryRange(), job.getExperienceRequirement(),
                job.getEducationRequirement(), job.getJobDescription(),
                job.getJobRequirement(), job.getTechStack(), analysisData);

        String response;
        try {
            response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("职位匹配 AI 调用失败", e);
            throw new BusinessException(ErrorCode.JOB_MATCH_ERROR);
        }

        Map<String, Object> result;
        try {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            result = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("职位匹配结果解析失败: {}", response, e);
            throw new BusinessException(ErrorCode.JOB_MATCH_ERROR);
        }

        int matchScore = toInt(result.get("matchScore"));
        int growthScore = toInt(result.get("growthScore"));
        int techGrowthScore = toInt(result.get("techGrowthScore"));
        int salaryCityScore = toInt(result.get("salaryCityScore"));
        int experienceFitScore = toInt(result.get("experienceFitScore"));
        int totalScore = jobRecommendService.calculateTotalScore(matchScore, growthScore, techGrowthScore, salaryCityScore, experienceFitScore);

        JobMatch jobMatch = new JobMatch();
        jobMatch.setUserId(userId);
        jobMatch.setResumeId(resumeId);
        jobMatch.setJobId(jobId);
        jobMatch.setMatchScore(matchScore);
        jobMatch.setGrowthScore(growthScore);
        jobMatch.setTechGrowthScore(techGrowthScore);
        jobMatch.setSalaryCityScore(salaryCityScore);
        jobMatch.setExperienceFitScore(experienceFitScore);
        jobMatch.setTotalScore(totalScore);
        jobMatch.setRecommendation((String) result.get("recommendation"));
        jobMatch.setReason((String) result.get("reason"));
        try {
            Object gapsObj = result.get("gaps");
            jobMatch.setGaps(gapsObj != null ? objectMapper.writeValueAsString(gapsObj) : "[]");
        } catch (Exception e) {
            jobMatch.setGaps("[]");
        }
        jobMatchMapper.insert(jobMatch);
        return jobMatch;
    }

    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}

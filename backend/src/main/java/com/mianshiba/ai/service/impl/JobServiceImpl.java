package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CompanyCertificationMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobFavoriteMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.JobMatchMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.job.JobImportRequest;
import com.mianshiba.ai.model.dto.job.JobListQueryRequest;
import com.mianshiba.ai.model.dto.job.JobMatchRequest;
import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.CompanyCertification;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.entity.JobFavorite;
import com.mianshiba.ai.model.entity.JobMatch;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.job.CompanyCertificationVO;
import com.mianshiba.ai.model.vo.job.CompanyVO;
import com.mianshiba.ai.model.vo.job.JobAnalysisVO;
import com.mianshiba.ai.model.vo.job.JobGapAnalysisVO;
import com.mianshiba.ai.model.vo.job.JobImportResultVO;
import com.mianshiba.ai.model.vo.job.JobKeywordVO;
import com.mianshiba.ai.model.vo.job.JobMatchVO;
import com.mianshiba.ai.model.vo.job.JobQuestionPredictionVO;
import com.mianshiba.ai.model.vo.job.JobVO;
import com.mianshiba.ai.service.AiJobAnalysisService;
import com.mianshiba.ai.service.JobCrawlService;
import com.mianshiba.ai.service.JobParseService;
import com.mianshiba.ai.service.JobRecommendService;
import com.mianshiba.ai.service.JobService;
import com.mianshiba.ai.service.ResumeJobMatchService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private static final Pattern JSON_CODE_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final JobCrawlService jobCrawlService;
    private final JobParseService jobParseService;
    private final AiJobAnalysisService aiJobAnalysisService;
    private final JobRecommendService jobRecommendService;
    private final ResumeJobMatchService resumeJobMatchService;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final CompanyCertificationMapper companyCertificationMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final JobMatchMapper jobMatchMapper;
    private final JobFavoriteMapper jobFavoriteMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobImportResultVO importUrl(String authorizationHeader, JobImportRequest request) {
        resolveUserId(authorizationHeader);

        JobCrawlService.CrawlResult crawlResult = jobCrawlService.crawl(request.getUrl().trim());

        JobImportResultVO result = new JobImportResultVO();

        if ("job".equals(request.getImportType())) {
            Job job = jobParseService.parseJob(crawlResult);
            job.setSourceUrl(crawlResult.finalUrl());
            job.setSourcePlatform(crawlResult.sourcePlatform());
            jobMapper.insert(job);

            Company company = jobParseService.parseCompany(crawlResult);
            if (company != null && company.getName() != null) {
                Company existing = companyMapper.selectOne(
                        Wrappers.lambdaQuery(Company.class)
                                .eq(Company::getName, company.getName())
                                .last("LIMIT 1"));
                if (existing == null) {
                    company.setSourceUrl(crawlResult.finalUrl());
                    companyMapper.insert(company);
                    job.setCompanyId(company.getId());
                    jobMapper.updateById(job);
                } else {
                    job.setCompanyId(existing.getId());
                    jobMapper.updateById(job);
                    company = existing;
                }
                result.setCompanyId(company.getId());
                result.setCompany(toCompanyVO(company, Collections.emptyList()));
            }

            JobAnalysis analysis = aiJobAnalysisService.analyzeJob(job);
            jobAnalysisMapper.insert(analysis);

            result.setResultType("job");
            result.setJobId(job.getId());
            result.setJob(toJobVO(job, null, null, null, false));
        } else {
            Company company = jobParseService.parseCompany(crawlResult);
            if (company == null) {
                throw new BusinessException(ErrorCode.JOB_PARSE_ERROR, "无法从该页面解析公司信息");
            }
            Company existing = companyMapper.selectOne(
                    Wrappers.lambdaQuery(Company.class)
                            .eq(Company::getName, company.getName())
                            .last("LIMIT 1"));
            if (existing == null) {
                company.setSourceUrl(crawlResult.finalUrl());
                companyMapper.insert(company);
                result.setCompanyId(company.getId());
                result.setCompany(toCompanyVO(company, Collections.emptyList()));
            } else {
                result.setCompanyId(existing.getId());
                List<CompanyCertification> certs = companyCertificationMapper.selectList(
                        Wrappers.lambdaQuery(CompanyCertification.class)
                                .eq(CompanyCertification::getCompanyId, existing.getId()));
                result.setCompany(toCompanyVO(existing, certs));
            }
            result.setResultType("company");
        }
        return result;
    }

    @Override
    public JobVO getJob(String authorizationHeader, Long jobId) {
        Long userId = resolveUserId(authorizationHeader);

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
        }

        CompanyVO companyVO = null;
        if (job.getCompanyId() != null) {
            Company company = companyMapper.selectById(job.getCompanyId());
            if (company != null) {
                List<CompanyCertification> certs = companyCertificationMapper.selectList(
                        Wrappers.lambdaQuery(CompanyCertification.class)
                                .eq(CompanyCertification::getCompanyId, company.getId()));
                companyVO = toCompanyVO(company, certs);
            }
        }

        JobAnalysisVO analysisVO = null;
        JobAnalysis analysis = jobAnalysisMapper.selectOne(
                Wrappers.lambdaQuery(JobAnalysis.class)
                        .eq(JobAnalysis::getJobId, jobId)
                        .last("LIMIT 1"));
        if (analysis != null) {
            analysisVO = toJobAnalysisVO(analysis);
        }

        JobMatchVO matchVO = null;
        JobMatch latestMatch = jobMatchMapper.selectOne(
                Wrappers.lambdaQuery(JobMatch.class)
                        .eq(JobMatch::getUserId, userId)
                        .eq(JobMatch::getJobId, jobId)
                        .orderByDesc(JobMatch::getCreateTime)
                        .last("LIMIT 1"));
        if (latestMatch != null) {
            matchVO = toJobMatchVO(latestMatch);
        }

        boolean favorited = jobFavoriteMapper.selectCount(
                Wrappers.lambdaQuery(JobFavorite.class)
                        .eq(JobFavorite::getUserId, userId)
                        .eq(JobFavorite::getJobId, jobId)) > 0;

        return toJobVO(job, companyVO, analysisVO, matchVO, favorited);
    }

    @Override
    public CompanyVO getCompany(String authorizationHeader, Long companyId) {
        resolveUserId(authorizationHeader);

        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND_ERROR);
        }

        List<CompanyCertification> certs = companyCertificationMapper.selectList(
                Wrappers.lambdaQuery(CompanyCertification.class)
                        .eq(CompanyCertification::getCompanyId, companyId));
        return toCompanyVO(company, certs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobMatchVO matchJob(String authorizationHeader, Long jobId, JobMatchRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        JobMatch jobMatch = resumeJobMatchService.match(userId, request.getResumeId(), jobId);
        return toJobMatchVO(jobMatch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteJob(String authorizationHeader, Long jobId) {
        Long userId = resolveUserId(authorizationHeader);
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
        }
        boolean exists = jobFavoriteMapper.selectCount(
                Wrappers.lambdaQuery(JobFavorite.class)
                        .eq(JobFavorite::getUserId, userId)
                        .eq(JobFavorite::getJobId, jobId)) > 0;
        if (!exists) {
            JobFavorite favorite = new JobFavorite();
            favorite.setUserId(userId);
            favorite.setJobId(jobId);
            jobFavoriteMapper.insert(favorite);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteJob(String authorizationHeader, Long jobId) {
        Long userId = resolveUserId(authorizationHeader);
        jobFavoriteMapper.delete(
                Wrappers.lambdaQuery(JobFavorite.class)
                        .eq(JobFavorite::getUserId, userId)
                        .eq(JobFavorite::getJobId, jobId));
    }

    @Override
    public List<JobVO> listFavorites(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        List<JobFavorite> favorites = jobFavoriteMapper.selectList(
                Wrappers.lambdaQuery(JobFavorite.class)
                        .eq(JobFavorite::getUserId, userId)
                        .orderByDesc(JobFavorite::getCreateTime));
        return favorites.stream().map(fav -> {
            Job job = jobMapper.selectById(fav.getJobId());
            if (job == null) {
                return null;
            }
            CompanyVO companyVO = null;
            if (job.getCompanyId() != null) {
                Company company = companyMapper.selectById(job.getCompanyId());
                if (company != null) {
                    companyVO = toCompanyVO(company, Collections.emptyList());
                }
            }
            return toJobVO(job, companyVO, null, null, true);
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<JobVO> listJobs(String authorizationHeader, JobListQueryRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        List<JobFavorite> favorites = jobFavoriteMapper.selectList(
                Wrappers.lambdaQuery(JobFavorite.class)
                        .eq(JobFavorite::getUserId, userId));
        List<Long> jobIds = favorites.stream()
                .map(JobFavorite::getJobId)
                .collect(Collectors.toList());
        if (jobIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Job> wrapper = Wrappers.lambdaQuery(Job.class)
                .in(Job::getId, jobIds);
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Job::getTitle, request.getKeyword());
        }
        if (StringUtils.hasText(request.getCity())) {
            wrapper.eq(Job::getCity, request.getCity());
        }
        if (StringUtils.hasText(request.getTechStack())) {
            wrapper.like(Job::getTechStack, request.getTechStack());
        }
        if (StringUtils.hasText(request.getApplicationStatus())) {
            wrapper.eq(Job::getApplicationStatus, request.getApplicationStatus());
        }
        wrapper.orderByDesc(Job::getCreateTime);
        List<Job> jobs = jobMapper.selectList(wrapper);
        return jobs.stream().map(job -> {
            CompanyVO companyVO = null;
            if (job.getCompanyId() != null) {
                Company company = companyMapper.selectById(job.getCompanyId());
                if (company != null) {
                    companyVO = toCompanyVO(company, Collections.emptyList());
                }
            }
            boolean favorited = jobFavoriteMapper.selectCount(
                    Wrappers.lambdaQuery(JobFavorite.class)
                            .eq(JobFavorite::getUserId, userId)
                            .eq(JobFavorite::getJobId, job.getId())) > 0;
            return toJobVO(job, companyVO, null, null, favorited);
        }).collect(Collectors.toList());
    }

    @Override
    public JobKeywordVO extractKeywords(String authorizationHeader, Long jobId) {
        Long userId = resolveUserId(authorizationHeader);
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
        }
        if (StringUtils.hasText(job.getKeywordsJson())) {
            try {
                return objectMapper.readValue(job.getKeywordsJson(), JobKeywordVO.class);
            } catch (Exception e) {
                log.warn("解析缓存的关键词 JSON 失败，重新提取", e);
            }
        }
        String prompt = "你是一位资深的 JD 分析专家。请从以下职位描述中提取关键词信息。\n\n" +
                "职位名称：%s\n岗位职责：%s\n岗位要求：%s\n技术栈：%s\n\n" +
                "请返回 JSON 格式，包含：\n" +
                "- hardSkills：硬技能列表\n" +
                "- softSkills：软技能列表\n" +
                "- bonusSkills：加分项列表\n" +
                "- hiddenRequirements：隐性要求列表\n\n" +
                "直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";
        String userMessage = String.format(prompt,
                nullToEmpty(job.getTitle()),
                nullToEmpty(job.getJobDescription()),
                nullToEmpty(job.getJobRequirement()),
                nullToEmpty(job.getTechStack()));
        String aiResponse = callAi(userMessage);
        String json = extractJsonFromResponse(aiResponse);
        JobKeywordVO vo;
        try {
            vo = objectMapper.readValue(json, JobKeywordVO.class);
        } catch (Exception e) {
            log.error("解析关键词提取 JSON 失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        try {
            job.setKeywordsJson(objectMapper.writeValueAsString(vo));
            jobMapper.updateById(job);
        } catch (Exception e) {
            log.warn("缓存关键词 JSON 失败", e);
        }
        return vo;
    }

    @Override
    public JobGapAnalysisVO analyzeGap(String authorizationHeader, Long jobId, Long resumeId) {
        Long userId = resolveUserId(authorizationHeader);
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
        }
        if (resumeId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历 ID 不能为空");
        }
        Resume resume = resumeMapper.selectOne(
                Wrappers.lambdaQuery(Resume.class)
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "简历不存在");
        }
        List<ResumeSection> sections = resumeSectionMapper.selectList(
                Wrappers.lambdaQuery(ResumeSection.class)
                        .eq(ResumeSection::getResumeId, resumeId)
                        .orderByAsc(ResumeSection::getSortOrder));
        StringBuilder resumeSummary = new StringBuilder();
        for (ResumeSection section : sections) {
            resumeSummary.append(section.getSectionType()).append(": ");
            if (section.getSectionData() != null) {
                resumeSummary.append(objectMapper.valueToTree(section.getSectionData()).toString());
            }
            resumeSummary.append("\n");
        }
        String prompt = "你是一位资深的简历和 JD 对比专家。请对比以下 JD 和简历内容，分析匹配差距。\n\n" +
                "JD 技术栈：%s\nJD 要求：%s\n\n" +
                "简历模块摘要：%s\n\n" +
                "请返回 JSON 格式：\n" +
                "- keywordCoverage：关键词覆盖率 0-100\n" +
                "- matchedKeywords：已匹配的关键词列表\n" +
                "- missingKeywords：缺失的关键词列表\n" +
                "- projectExpressionGaps：项目表达缺口列表\n" +
                "- optimizeActions：具体优化动作列表\n\n" +
                "直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";
        String userMessage = String.format(prompt,
                nullToEmpty(job.getTechStack()),
                nullToEmpty(job.getJobRequirement()),
                resumeSummary.toString());
        String aiResponse = callAi(userMessage);
        String json = extractJsonFromResponse(aiResponse);
        JobGapAnalysisVO vo;
        try {
            vo = objectMapper.readValue(json, JobGapAnalysisVO.class);
        } catch (Exception e) {
            log.error("解析差距分析 JSON 失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        return vo;
    }

    @Override
    public JobQuestionPredictionVO predictQuestions(String authorizationHeader, Long jobId) {
        Long userId = resolveUserId(authorizationHeader);
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
        }
        if (StringUtils.hasText(job.getPredictedQuestionsJson())) {
            try {
                return objectMapper.readValue(job.getPredictedQuestionsJson(), JobQuestionPredictionVO.class);
            } catch (Exception e) {
                log.warn("解析缓存的预测面试题 JSON 失败，重新生成", e);
            }
        }
        JobAnalysis analysis = jobAnalysisMapper.selectOne(
                Wrappers.lambdaQuery(JobAnalysis.class)
                        .eq(JobAnalysis::getJobId, jobId)
                        .last("LIMIT 1"));
        String requirementSummary = analysis != null ? nullToEmpty(analysis.getRequirementSummary()) : "";
        String interviewFocus = analysis != null ? nullToEmpty(analysis.getInterviewFocus()) : "";
        String prompt = "你是一位资深的技术面试官。请根据以下职位信息预测可能的面试题。\n\n" +
                "职位名称：%s\n公司：%s\n技术栈：%s\n岗位要求总结：%s\n面试准备重点：%s\n\n" +
                "请返回 JSON 格式：\n" +
                "- technicalQuestions：技术面试题 3-5 道\n" +
                "- projectQuestions：项目相关题 2-3 道\n" +
                "- systemDesignQuestions：系统设计题 1-2 道\n" +
                "- hrQuestions：HR 面试题 2-3 道\n\n" +
                "直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";
        String userMessage = String.format(prompt,
                nullToEmpty(job.getTitle()),
                nullToEmpty(job.getCompanyName()),
                nullToEmpty(job.getTechStack()),
                requirementSummary,
                interviewFocus);
        String aiResponse = callAi(userMessage);
        String json = extractJsonFromResponse(aiResponse);
        JobQuestionPredictionVO vo;
        try {
            vo = objectMapper.readValue(json, JobQuestionPredictionVO.class);
        } catch (Exception e) {
            log.error("解析预测面试题 JSON 失败: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        try {
            job.setPredictedQuestionsJson(objectMapper.writeValueAsString(vo));
            jobMapper.updateById(job);
        } catch (Exception e) {
            log.warn("缓存预测面试题 JSON 失败", e);
        }
        return vo;
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        return user.getId();
    }

    private JobVO toJobVO(Job job, CompanyVO company, JobAnalysisVO analysis, JobMatchVO matchResult, boolean favorited) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setCompany(company);
        vo.setCompanyName(job.getCompanyName());
        vo.setTitle(job.getTitle());
        vo.setSourcePlatform(job.getSourcePlatform());
        vo.setSourceUrl(job.getSourceUrl());
        vo.setCity(job.getCity());
        vo.setSalaryRange(job.getSalaryRange());
        vo.setExperienceRequirement(job.getExperienceRequirement());
        vo.setEducationRequirement(job.getEducationRequirement());
        vo.setJobDescription(job.getJobDescription());
        vo.setJobRequirement(job.getJobRequirement());
        vo.setTechStack(job.getTechStack());
        vo.setStatus(job.getStatus());
        vo.setAnalysis(analysis);
        vo.setMatchResult(matchResult);
        vo.setFavorited(favorited);
        return vo;
    }

    private CompanyVO toCompanyVO(Company company, List<CompanyCertification> certs) {
        CompanyVO vo = new CompanyVO();
        vo.setId(company.getId());
        vo.setName(company.getName());
        vo.setNormalizedName(company.getNormalizedName());
        vo.setWebsite(company.getWebsite());
        vo.setIndustry(company.getIndustry());
        vo.setCity(company.getCity());
        vo.setScale(company.getScale());
        vo.setDescription(company.getDescription());
        vo.setMainBusiness(company.getMainBusiness());
        vo.setTechDirection(company.getTechDirection());
        vo.setIsSpecializedNew(company.getIsSpecializedNew() != null && company.getIsSpecializedNew() == 1);
        vo.setIsLittleGiant(company.getIsLittleGiant() != null && company.getIsLittleGiant() == 1);
        vo.setCertificationConfidence(company.getCertificationConfidence());
        vo.setCertifications(certs.stream().map(this::toCertificationVO).collect(Collectors.toList()));
        return vo;
    }

    private CompanyCertificationVO toCertificationVO(CompanyCertification cert) {
        CompanyCertificationVO vo = new CompanyCertificationVO();
        vo.setId(cert.getId());
        vo.setCertificationType(cert.getCertificationType());
        vo.setStatus(cert.getStatus());
        vo.setEvidenceSource(cert.getEvidenceSource());
        vo.setEvidenceUrl(cert.getEvidenceUrl());
        vo.setEvidenceText(cert.getEvidenceText());
        vo.setConfidenceScore(cert.getConfidenceScore());
        return vo;
    }

    private JobAnalysisVO toJobAnalysisVO(JobAnalysis analysis) {
        JobAnalysisVO vo = new JobAnalysisVO();
        vo.setId(analysis.getId());
        vo.setRequirementSummary(analysis.getRequirementSummary());
        vo.setCoreSkills(analysis.getCoreSkills());
        vo.setHiddenRequirements(analysis.getHiddenRequirements());
        vo.setInterviewFocus(analysis.getInterviewFocus());
        vo.setResumeSuggestions(analysis.getResumeSuggestions());
        vo.setRiskPoints(analysis.getRiskPoints());
        return vo;
    }

    private JobMatchVO toJobMatchVO(JobMatch match) {
        JobMatchVO vo = new JobMatchVO();
        vo.setId(match.getId());
        vo.setMatchScore(match.getMatchScore());
        vo.setGrowthScore(match.getGrowthScore());
        vo.setTechGrowthScore(match.getTechGrowthScore());
        vo.setSalaryCityScore(match.getSalaryCityScore());
        vo.setExperienceFitScore(match.getExperienceFitScore());
        vo.setTotalScore(match.getTotalScore());
        vo.setRecommendation(match.getRecommendation());
        vo.setReason(match.getReason());
        vo.setGaps(match.getGaps());
        return vo;
    }

    private String callAi(String userMessage) {
        try {
            return chatClient.prompt()
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

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}

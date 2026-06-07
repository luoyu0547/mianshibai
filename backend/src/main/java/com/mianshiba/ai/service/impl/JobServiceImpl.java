package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CompanyCertificationMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobFavoriteMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.JobMatchMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.job.JobImportRequest;
import com.mianshiba.ai.model.dto.job.JobMatchRequest;
import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.CompanyCertification;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.entity.JobFavorite;
import com.mianshiba.ai.model.entity.JobMatch;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.job.CompanyCertificationVO;
import com.mianshiba.ai.model.vo.job.CompanyVO;
import com.mianshiba.ai.model.vo.job.JobAnalysisVO;
import com.mianshiba.ai.model.vo.job.JobImportResultVO;
import com.mianshiba.ai.model.vo.job.JobMatchVO;
import com.mianshiba.ai.model.vo.job.JobVO;
import com.mianshiba.ai.service.AiJobAnalysisService;
import com.mianshiba.ai.service.JobCrawlService;
import com.mianshiba.ai.service.JobParseService;
import com.mianshiba.ai.service.JobRecommendService;
import com.mianshiba.ai.service.JobService;
import com.mianshiba.ai.service.ResumeJobMatchService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

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
    private final ObjectMapper objectMapper;

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
}

package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobPageFetchService;
import com.mianshiba.ai.service.JobSourceDiscoveryService;
import com.mianshiba.ai.service.JobSourcingAgentService;
import com.mianshiba.ai.service.JobSourcingExtractService;
import com.mianshiba.ai.service.JobSourcingQualityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobSourcingAgentServiceImpl implements JobSourcingAgentService {

    private static final String RUNNING = "running";
    private static final String SUCCESS = "success";
    private static final String PARTIAL_SUCCESS = "partial_success";
    private static final String FAILED = "failed";
    private static final String EXTRACTED = "extracted";
    private static final String PENDING_REVIEW = "pending_review";
    private static final String ITEM_FAILED = "failed";
    private static final String NO_VALID_SOURCE_ERROR = "未发现有效职位来源，请配置招聘平台职位页、职位列表页或企业官网招聘页";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JobCrawlTaskMapper taskMapper;
    private final JobCrawlRunMapper runMapper;
    private final JobCrawlItemMapper itemMapper;
    private final JobSourceDiscoveryService discoveryService;
    private final JobPageFetchService fetchService;
    private final JobSourcingExtractService extractService;
    private final JobSourcingQualityService qualityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobCrawlRun runTask(Long taskId) {
        JobCrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        JobCrawlRun run = new JobCrawlRun();
        run.setTaskId(taskId);
        run.setStatus(RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setTotalCount(0);
        runMapper.insert(run);

        List<JobSourceDiscoveryService.CandidateUrl> urls = discoveryService.discover(task);
        if (urls == null) {
            urls = Collections.emptyList();
        }
        run.setTotalCount(urls.size());
        runMapper.updateById(run);

        if (urls.isEmpty()) {
            run.setSuccessCount(0);
            run.setFailedCount(0);
            run.setStatus(FAILED);
            run.setErrorMessage(NO_VALID_SOURCE_ERROR);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            return run;
        }

        int success = 0;
        int failed = 0;

        for (JobSourceDiscoveryService.CandidateUrl candidateUrl : urls) {
            String url = candidateUrl.url();
            JobCrawlItem item = new JobCrawlItem();
            item.setRunId(run.getId());
            item.setTaskId(taskId);
            item.setSourceUrl(url);
            item.setReviewStatus(PENDING_REVIEW);
            itemMapper.insert(item);

            try {
                JobPageFetchService.FetchedPage page = fetchService.fetch(url);
                JobSourcingExtractService.ExtractedJobCard card = extractService.extract(page);
                JobSourcingQualityService.QualityResult qualityResult = qualityService.score(card);

                item.setRawTitle(card.title());
                item.setRawCompanyName(card.companyName());
                item.setSourcePlatform(page.sourcePlatform());
                item.setRawContent(page.content());
                item.setSummary(card.summary());
                item.setQualityScore(qualityResult.qualityScore());
                item.setConfidenceScore(card.confidenceScore());
                item.setExtractedJson(buildExtractedJson(card, qualityResult));
                item.setTagsJson(card.tagsJson());
                item.setStatus(EXTRACTED);
                itemMapper.updateById(item);
                success++;
            } catch (Exception e) {
                log.error("Sourcing item failed: {}", url, e);
                item.setStatus(ITEM_FAILED);
                item.setReviewStatus(ITEM_FAILED);
                String errorMsg = e.getMessage();
                item.setErrorMessage(errorMsg != null && errorMsg.length() > 1024
                        ? errorMsg.substring(0, 1024) : errorMsg);
                itemMapper.updateById(item);
                failed++;
            }
        }

        run.setSuccessCount(success);
        run.setFailedCount(failed);
        run.setFinishedAt(LocalDateTime.now());
        if (failed == run.getTotalCount()) {
            run.setStatus(FAILED);
        } else if (failed > 0) {
            run.setStatus(PARTIAL_SUCCESS);
        } else {
            run.setStatus(SUCCESS);
        }
        runMapper.updateById(run);

        return run;
    }

    private String buildExtractedJson(JobSourcingExtractService.ExtractedJobCard card, JobSourcingQualityService.QualityResult quality) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", card.title());
        data.put("companyName", card.companyName());
        data.put("city", card.city());
        data.put("salaryRange", card.salaryRange());
        data.put("experienceRequirement", card.experienceRequirement());
        data.put("educationRequirement", card.educationRequirement());
        data.put("jobDescription", card.jobDescription());
        data.put("jobRequirement", card.jobRequirement());
        data.put("techStack", card.techStackJson());
        data.put("summary", card.summary());
        data.put("tags", card.tagsJson());
        data.put("qualityScore", quality.qualityScore());
        data.put("warnings", quality.warningsJson());
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("序列化抽取结果失败", e);
            return "{}";
        }
    }
}

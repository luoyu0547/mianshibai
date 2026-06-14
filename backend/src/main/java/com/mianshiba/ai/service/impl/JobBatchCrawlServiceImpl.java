package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.AiJobAnalysisService;
import com.mianshiba.ai.service.JobBatchCrawlService;
import com.mianshiba.ai.service.JobCrawlService;
import com.mianshiba.ai.service.JobDedupService;
import com.mianshiba.ai.service.JobParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobBatchCrawlServiceImpl implements JobBatchCrawlService {

    private final JobCrawlTaskMapper taskMapper;
    private final JobCrawlRunMapper runMapper;
    private final JobCrawlItemMapper itemMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final JobAnalysisMapper jobAnalysisMapper;
    private final JobCrawlService jobCrawlService;
    private final JobParseService jobParseService;
    private final AiJobAnalysisService aiJobAnalysisService;
    private final JobDedupService jobDedupService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobCrawlRun runTask(Long taskId) {
        JobCrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        JobCrawlRun run = new JobCrawlRun();
        run.setTaskId(taskId);
        run.setStatus("running");
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);

        List<String> urls = resolveUrls(task);
        run.setTotalCount(urls.size());
        runMapper.updateById(run);

        int success = 0;
        int duplicate = 0;
        int failed = 0;

        for (String url : urls) {
            JobCrawlItem item = new JobCrawlItem();
            item.setRunId(run.getId());
            item.setTaskId(taskId);
            item.setSourceUrl(url);
            item.setNormalizedUrl(jobDedupService.normalizeUrl(url));
            item.setStatus("pending");
            itemMapper.insert(item);

            try {
                JobCrawlService.CrawlResult crawlResult = jobCrawlService.crawl(url);
                item.setRawTitle(crawlResult.title());
                item.setRawCompanyName(detectCompanyFromTitle(crawlResult.title()));
                item.setNormalizedUrl(crawlResult.finalUrl());
                itemMapper.updateById(item);

                Job job = jobParseService.parseJob(crawlResult);
                job.setSourceUrl(crawlResult.finalUrl());
                job.setSourcePlatform(crawlResult.sourcePlatform());

                Job existing = jobDedupService.findDuplicate(item.getNormalizedUrl(), job);
                if (existing != null) {
                    item.setJobId(existing.getId());
                    item.setStatus("duplicate");
                    itemMapper.updateById(item);
                    duplicate++;
                    continue;
                }

                jobMapper.insert(job);

                Company company = jobParseService.parseCompany(crawlResult);
                if (company != null && company.getName() != null) {
                    Company existingCompany = companyMapper.selectOne(
                            Wrappers.lambdaQuery(Company.class)
                                    .eq(Company::getName, company.getName())
                                    .last("LIMIT 1"));
                    if (existingCompany == null) {
                        companyMapper.insert(company);
                        job.setCompanyId(company.getId());
                    } else {
                        job.setCompanyId(existingCompany.getId());
                    }
                    jobMapper.updateById(job);
                }

                try {
                    JobAnalysis analysis = aiJobAnalysisService.analyzeJob(job);
                    jobAnalysisMapper.insert(analysis);
                } catch (Exception e) {
                    log.warn("AI analysis failed for job {}", job.getId(), e);
                }

                item.setJobId(job.getId());
                item.setStatus("success");
                itemMapper.updateById(item);
                success++;

            } catch (Exception e) {
                log.error("Crawl item failed: {}", url, e);
                item.setStatus("failed");
                String errorMsg = e.getMessage();
                item.setErrorMessage(errorMsg != null && errorMsg.length() > 1024
                        ? errorMsg.substring(0, 1024) : errorMsg);
                itemMapper.updateById(item);
                failed++;
            }
        }

        run.setSuccessCount(success);
        run.setDuplicateCount(duplicate);
        run.setFailedCount(failed);
        run.setFinishedAt(LocalDateTime.now());
        if (failed == run.getTotalCount()) {
            run.setStatus("failed");
        } else if (failed > 0) {
            run.setStatus("partial_success");
        } else {
            run.setStatus("success");
        }
        runMapper.updateById(run);

        return run;
    }

    private List<String> resolveUrls(JobCrawlTask task) {
        if ("manual_url_list".equals(task.getSourceType())) {
            String config = task.getConfigJson() != null ? task.getConfigJson().toString() : "";
            return Arrays.stream(config.split("\\R"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return task.getSourceUrl() != null && StringUtils.hasText(task.getSourceUrl())
                ? List.of(task.getSourceUrl()) : List.of();
    }

    private String detectCompanyFromTitle(String title) {
        if (title == null) return "";
        int idx = title.indexOf(" - ");
        if (idx > 0) return title.substring(0, idx).trim();
        idx = title.indexOf(" | ");
        if (idx > 0) return title.substring(0, idx).trim();
        return "";
    }
}

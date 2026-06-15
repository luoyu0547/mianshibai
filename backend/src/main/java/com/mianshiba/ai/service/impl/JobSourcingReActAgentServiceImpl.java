package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.model.enums.JobSourcingRunStatus;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobPlatformAdapter;
import com.mianshiba.ai.service.JobPlatformAdapterRegistry;
import com.mianshiba.ai.service.JobSourcingReActAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 职位采集 ReAct Agent 服务实现
 * 负责驱动平台适配器完成发现、抓取、解析与质量评分。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobSourcingReActAgentServiceImpl implements JobSourcingReActAgentService {

    /**
     * 采集任务 Mapper
     */
    private final JobCrawlTaskMapper taskMapper;

    /**
     * 采集运行记录 Mapper
     */
    private final JobCrawlRunMapper runMapper;

    /**
     * 采集项 Mapper
     */
    private final JobCrawlItemMapper itemMapper;

    /**
     * 平台适配器注册表
     */
    private final JobPlatformAdapterRegistry adapterRegistry;

    /**
     * 职位采集配置
     */
    private final JobSourcingProperties properties;

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    @Override
    public JobCrawlRun runTask(Long taskId) {
        // 1. 查询采集任务
        JobCrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 2. 初始化采集运行记录
        JobCrawlRun run = new JobCrawlRun();
        run.setTaskId(taskId);
        run.setStatus(JobSourcingRunStatus.RUNNING.getValue());
        run.setStartedAt(LocalDateTime.now());
        run.setTotalCount(0);
        runMapper.insert(run);

        // 3. 解析任务目标参数
        String platform = task.getSourceType();
        String keywords = task.getKeywords() != null ? task.getKeywords() : "";
        String cities = task.getCities() != null ? task.getCities() : "";
        String experienceLevels = task.getExperienceLevels() != null ? task.getExperienceLevels() : "";
        int maxPages = properties.getMaxPagesPerSource();
        int targetCount = properties.getTargetCount();

        // 4. 获取平台适配器；旧版本遗留任务可能保存了不再支持的平台值
        JobPlatformAdapter adapter;
        try {
            adapter = adapterRegistry.getAdapter(platform);
        } catch (IllegalArgumentException e) {
            run.setStatus(JobSourcingRunStatus.FAILED.getValue());
            run.setErrorMessage(e.getMessage());
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            return run;
        }

        // 5. 授权检查
        if (adapter.requiresAuth()) {
            BrowserSessionService.AuthCheckResult authResult = adapter.checkAuth();
            if (!"authorized".equals(authResult.status())) {
                run.setStatus(JobSourcingRunStatus.AUTH_REQUIRED.getValue());
                run.setErrorMessage("平台 " + platform + " 需要完成授权");
                run.setFinishedAt(LocalDateTime.now());
                runMapper.updateById(run);
                return run;
            }
        }

        // 6. 发现职位列表
        JobDiscoveryRequest request = new JobDiscoveryRequest(platform, keywords, cities, experienceLevels, maxPages, targetCount);
        List<JobListEntry> entries = adapter.discover(request);

        // 7. 更新职位总数
        run.setTotalCount(entries.size());
        runMapper.updateById(run);

        // 8. 遍历处理每个职位
        int success = 0;
        int failed = 0;

        for (JobListEntry entry : entries) {
            // 8.1 初始化采集项
            JobCrawlItem item = new JobCrawlItem();
            item.setRunId(run.getId());
            item.setTaskId(taskId);
            item.setSourceUrl(entry.sourceUrl());
            item.setNormalizedUrl(entry.sourceUrl());
        item.setReviewStatus("pending_review");
                item.setStatus(JobSourcingRunStatus.RUNNING.getValue());
            itemMapper.insert(item);

            try {
                // 8.2 抓取详情页
                FetchedJobPage page = adapter.fetchDetail(entry.sourceUrl());
                // 8.3 兜底解析
                ExtractedJobCard card = adapter.fallbackExtract(page);
                // 8.4 质量评分
                int qualityScore = scoreQuality(card, task);

                // 8.5 更新采集项为已提取
                item.setRawTitle(card.title());
                item.setRawCompanyName(card.companyName());
                item.setSourcePlatform(platform);
                item.setRawContent(page.html());
                item.setSummary(card.summary());
                item.setQualityScore(qualityScore);
                item.setConfidenceScore(card.confidenceScore());
                item.setExtractedJson(buildExtractedJson(card, qualityScore));
                item.setTagsJson(card.tagsJson());
                item.setStatus("extracted");
                item.setReviewStatus("pending_review");
                item.setReviewStatus("pending_review");
                itemMapper.updateById(item);
                success++;
            } catch (Exception e) {
                log.error("Crawl item failed: {}", entry.sourceUrl(), e);
                item.setStatus("failed");
                item.setReviewStatus("failed");
                String errorMsg = e.getMessage();
                item.setErrorMessage(errorMsg != null && errorMsg.length() > 1024
                        ? errorMsg.substring(0, 1024) : errorMsg);
                itemMapper.updateById(item);
                failed++;
            }
        }

        // 9. 更新运行记录统计
        run.setSuccessCount(success);
        run.setFailedCount(failed);
        run.setFinishedAt(LocalDateTime.now());

        // 10. 判定最终状态
        if (success == 0 && failed > 0) {
            run.setStatus(JobSourcingRunStatus.FAILED.getValue());
        } else if (success > 0 && failed > 0) {
            run.setStatus(JobSourcingRunStatus.PARTIAL_SUCCESS.getValue());
        } else {
            run.setStatus(JobSourcingRunStatus.SUCCESS.getValue());
        }

        runMapper.updateById(run);
        return run;
    }

    /**
     * 对提取结果进行质量评分
     *
     * @param card 提取的职位卡片
     * @param task 采集任务
     * @return 质量分（0-100）
     */
    private int scoreQuality(ExtractedJobCard card, JobCrawlTask task) {
        int score = 80;
        if (card.title() == null || card.title().isBlank()
                || card.companyName() == null || card.companyName().isBlank()) {
            score -= 30;
        }
        if (card.city() == null || card.city().isBlank()) {
            score -= 10;
        }
        if (card.salaryRange() == null || card.salaryRange().isBlank()) {
            score -= 10;
        }
        if ((card.experienceRequirement() == null || card.experienceRequirement().isBlank())
                && (card.educationRequirement() == null || card.educationRequirement().isBlank())) {
            score -= 10;
        }
        return Math.max(0, score);
    }

    /**
     * 构建提取后的结构化 JSON
     *
     * @param card        提取的职位卡片
     * @param qualityScore 质量评分
     * @return JSON 字符串
     */
    private String buildExtractedJson(ExtractedJobCard card, int qualityScore) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", card.title());
        data.put("companyName", card.companyName());
        data.put("city", card.city());
        data.put("salaryRange", card.salaryRange());
        data.put("experienceRequirement", card.experienceRequirement());
        data.put("educationRequirement", card.educationRequirement());
        data.put("jobDescription", card.jobDescription());
        data.put("jobRequirement", card.jobRequirement());
        data.put("techStack", readJsonArray(card.techStackJson()));
        data.put("summary", card.summary());
        data.put("tags", readJsonArray(card.tagsJson()));
        data.put("confidenceScore", card.confidenceScore());
        data.put("qualityScore", qualityScore);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Build extracted json failed", e);
            return "{}";
        }
    }

    private JsonNode readJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.isArray() ? node : objectMapper.createArrayNode();
        } catch (Exception e) {
            log.warn("Parse json array failed: {}", json);
            return objectMapper.createArrayNode();
        }
    }
}

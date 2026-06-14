package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobAnalysisMapper;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobAnalysis;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.AiJobAnalysisService;
import com.mianshiba.ai.service.JobCrawlService;
import com.mianshiba.ai.service.JobDedupService;
import com.mianshiba.ai.service.JobParseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobBatchCrawlServiceImplTest {

    @Mock
    private JobCrawlTaskMapper taskMapper;
    @Mock
    private JobCrawlRunMapper runMapper;
    @Mock
    private JobCrawlItemMapper itemMapper;
    @Mock
    private JobMapper jobMapper;
    @Mock
    private CompanyMapper companyMapper;
    @Mock
    private JobAnalysisMapper jobAnalysisMapper;
    @Mock
    private JobCrawlService jobCrawlService;
    @Mock
    private JobParseService jobParseService;
    @Mock
    private AiJobAnalysisService aiJobAnalysisService;
    @Mock
    private JobDedupService jobDedupService;

    @InjectMocks
    private JobBatchCrawlServiceImpl batchCrawlService;

    @Test
    void runTask_shouldContinueWhenOneItemFails() {
        JobCrawlTask task = mockTaskWithUrls("https://example.com/job1\nhttps://example.com/job2\nhttps://example.com/job3");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(jobDedupService.normalizeUrl(any())).thenAnswer(inv -> inv.getArgument(0));

        JobCrawlService.CrawlResult cr1 = new JobCrawlService.CrawlResult(
                "https://example.com/job1", "https://example.com/job1", "Dev - Company A", "content1", "<html>1</html>", "platform");
        JobCrawlService.CrawlResult cr3 = new JobCrawlService.CrawlResult(
                "https://example.com/job3", "https://example.com/job3", "QA - Company C", "content3", "<html>3</html>", "platform");

        when(jobCrawlService.crawl("https://example.com/job1")).thenReturn(cr1);
        when(jobCrawlService.crawl("https://example.com/job2")).thenThrow(new RuntimeException("Network error"));
        when(jobCrawlService.crawl("https://example.com/job3")).thenReturn(cr3);

        Job job1 = new Job();
        job1.setTitle("Dev");
        job1.setCompanyName("Company A");
        when(jobParseService.parseJob(cr1)).thenReturn(job1);
        when(jobDedupService.findDuplicate(any(), any())).thenReturn(null);
        when(jobParseService.parseCompany(cr1)).thenReturn(null);

        Job job3 = new Job();
        job3.setTitle("QA");
        job3.setCompanyName("Company C");
        when(jobParseService.parseJob(cr3)).thenReturn(job3);
        when(jobDedupService.findDuplicate(any(), any())).thenReturn(null);
        when(jobParseService.parseCompany(cr3)).thenReturn(null);
        when(aiJobAnalysisService.analyzeJob(any(Job.class))).thenReturn(new JobAnalysis());

        batchCrawlService.runTask(1L);

        verify(runMapper).insert(any(JobCrawlRun.class));
    }

    @Test
    void runTask_shouldInsertJobForSuccessfulNewItem() {
        JobCrawlTask task = mockTaskWithUrls("https://example.com/job1");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(jobDedupService.normalizeUrl(any())).thenAnswer(inv -> inv.getArgument(0));

        JobCrawlService.CrawlResult cr = new JobCrawlService.CrawlResult(
                "https://example.com/job1", "https://example.com/job1/final", "Senior Dev - Best Corp", "content", "<html></html>", "linkedin");

        when(jobCrawlService.crawl("https://example.com/job1")).thenReturn(cr);

        Job parsedJob = new Job();
        parsedJob.setTitle("Senior Dev");
        parsedJob.setCompanyName("Best Corp");
        when(jobParseService.parseJob(cr)).thenReturn(parsedJob);
        when(jobDedupService.findDuplicate(any(), any())).thenReturn(null);

        Company company = new Company();
        company.setName("Best Corp");
        when(jobParseService.parseCompany(cr)).thenReturn(company);

        when(aiJobAnalysisService.analyzeJob(any(Job.class))).thenReturn(new JobAnalysis());

        batchCrawlService.runTask(1L);

        verify(jobMapper).insert(parsedJob);
        verify(jobAnalysisMapper).insert(any(JobAnalysis.class));
    }

    @Test
    void runTask_shouldMarkDuplicateWithoutInsertingJob() {
        JobCrawlTask task = mockTaskWithUrls("https://example.com/job1");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(jobDedupService.normalizeUrl(any())).thenAnswer(inv -> inv.getArgument(0));

        JobCrawlService.CrawlResult cr = new JobCrawlService.CrawlResult(
                "https://example.com/job1", "https://example.com/job1", "Dev", "content", "<html></html>", "platform");

        when(jobCrawlService.crawl("https://example.com/job1")).thenReturn(cr);

        Job parsedJob = new Job();
        parsedJob.setTitle("Dev");
        parsedJob.setCompanyName("Existing Corp");
        when(jobParseService.parseJob(cr)).thenReturn(parsedJob);

        Job existingJob = new Job();
        existingJob.setId(99L);
        when(jobDedupService.findDuplicate(any(), any())).thenReturn(existingJob);

        batchCrawlService.runTask(1L);

        verify(jobMapper, never()).insert(any(Job.class));
        verify(jobAnalysisMapper, never()).insert(any(JobAnalysis.class));
    }

    @Test
    void runTask_shouldThrowWhenTaskNotFound() {
        when(taskMapper.selectById(anyLong())).thenReturn(null);

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                com.mianshiba.ai.exception.BusinessException.class,
                () -> batchCrawlService.runTask(1L))
        ).isNotNull();
    }

    private JobCrawlTask mockTaskWithUrls(String urls) {
        JobCrawlTask task = new JobCrawlTask();
        task.setId(1L);
        task.setName("Test Task");
        task.setSourceType("manual_url_list");
        task.setSourceUrl(null);
        task.setConfigJson(urls);
        task.setStatus("enabled");
        return task;
    }
}

package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobPageFetchService;
import com.mianshiba.ai.service.JobSourceDiscoveryService;
import com.mianshiba.ai.service.JobSourcingExtractService;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.service.JobSourcingQualityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobSourcingAgentServiceImplTest {

    @Mock
    JobCrawlTaskMapper taskMapper;
    @Mock
    JobCrawlRunMapper runMapper;
    @Mock
    JobCrawlItemMapper itemMapper;
    @Mock
    JobSourceDiscoveryService discoveryService;
    @Mock
    JobPageFetchService fetchService;
    @Mock
    JobSourcingExtractService extractService;
    @Mock
    JobSourcingQualityService qualityService;
    @InjectMocks
    JobSourcingAgentServiceImpl service;

    @Test
    void runTask_shouldCreatePendingReviewItem() {
        JobCrawlTask task = new JobCrawlTask();
        task.setId(1L);
        task.setSourceType("public_feed");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(discoveryService.discover(task)).thenReturn(List.of(new JobSourceDiscoveryService.CandidateUrl("https://example.com/job/1", "public_search", "website", "Java \u676d\u5dde \u62db\u8058")));
        when(fetchService.fetch("https://example.com/job/1")).thenReturn(new JobPageFetchService.FetchedPage("https://example.com/job/1", "https://example.com/job/1", "Java", "content", "<html></html>", "website", false));
        when(extractService.extract(any())).thenReturn(new JobSourcingExtractService.ExtractedJobCard("Java\u540e\u7aef", "\u793a\u4f8b\u79d1\u6280", "\u676d\u5dde", "20-30K", "3-5\u5e74", "\u672c\u79d1", "\u5f00\u53d1\u540e\u7aef", "\u719f\u6089 Java", "[\"Java\"]", "\u540e\u7aef\u5c97\u4f4d", "[\"Java\u540e\u7aef\"]", 90));
        when(qualityService.score(any())).thenReturn(new JobSourcingQualityService.QualityResult(90, "[]"));

        JobCrawlRun run = service.runTask(1L);

        ArgumentCaptor<JobCrawlItem> itemCaptor = ArgumentCaptor.forClass(JobCrawlItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getReviewStatus()).isEqualTo("pending_review");
        assertThat(itemCaptor.getValue().getQualityScore()).isEqualTo(90);
        assertThat(itemCaptor.getValue().getExtractedJson()).isInstanceOf(String.class);
        assertThat((String) itemCaptor.getValue().getExtractedJson())
                .contains("\"title\"")
                .contains("Java\u540e\u7aef")
                .contains("\"qualityScore\"");
        assertThat(itemCaptor.getValue().getTagsJson()).isInstanceOf(String.class);
        assertThat(run.getStatus()).isEqualTo("success");
    }

    @Test
    void runTask_shouldSetRunStatusFailedWhenAllItemsFail() {
        JobCrawlTask task = new JobCrawlTask();
        task.setId(1L);
        task.setSourceType("public_feed");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(discoveryService.discover(task)).thenReturn(List.of(new JobSourceDiscoveryService.CandidateUrl("https://example.com/job/1", "public_search", "website", "Java")));
        when(fetchService.fetch("https://example.com/job/1")).thenReturn(new JobPageFetchService.FetchedPage("https://example.com/job/1", "https://example.com/job/1", "Java", "content", "<html></html>", "website", false));
        when(extractService.extract(any())).thenThrow(new RuntimeException("extraction failed"));

        JobCrawlRun run = service.runTask(1L);

        assertThat(run.getStatus()).isEqualTo("failed");
    }

    @Test
    void runTask_shouldFailWhenNoValidJobSourceDiscovered() {
        JobCrawlTask task = new JobCrawlTask();
        task.setId(1L);
        task.setKeywords("Java后端");
        task.setCities("深圳");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(discoveryService.discover(task)).thenReturn(List.of());

        JobCrawlRun run = service.runTask(1L);

        assertThat(run.getTotalCount()).isZero();
        assertThat(run.getSuccessCount()).isZero();
        assertThat(run.getFailedCount()).isZero();
        assertThat(run.getStatus()).isEqualTo("failed");
        assertThat(run.getErrorMessage()).isEqualTo("未发现有效职位来源，请配置招聘平台职位页、职位列表页或企业官网招聘页");
    }

    @Test
    void runTask_shouldThrowBusinessExceptionWhenTaskNotFound() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.runTask(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
    }
}

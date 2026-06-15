package com.mianshiba.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlItemReviewRequest;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSourcingReviewServiceImplTest {

    @Mock JobCrawlItemMapper itemMapper;
    @Mock JobMapper jobMapper;
    @Mock CompanyMapper companyMapper;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks JobSourcingReviewServiceImpl reviewService;

    @Test
    void approve_shouldPublishJobAndMarkApproved() {
        JobCrawlItem item = pendingItem(1L);
        when(itemMapper.selectById(1L)).thenReturn(item);
        doAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.setId(100L);
            return 1;
        }).when(jobMapper).insert(any(Job.class));

        AdminJobCrawlItemReviewRequest request = new AdminJobCrawlItemReviewRequest();
        reviewService.approve(1L, request);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobMapper).insert(jobCaptor.capture());
        assertThat(item.getReviewStatus()).isEqualTo("approved");
        assertThat(item.getJobId()).isNotNull();
        verify(itemMapper).updateById(item);
    }

    @Test
    void reject_shouldNotCreateJob() {
        JobCrawlItem item = pendingItem(1L);
        when(itemMapper.selectById(1L)).thenReturn(item);

        AdminJobCrawlItemReviewRequest request = new AdminJobCrawlItemReviewRequest();
        request.setReviewNote("公司信息不足");
        reviewService.reject(1L, request);

        assertThat(item.getReviewStatus()).isEqualTo("rejected");
        assertThat(item.getReviewNote()).isEqualTo("公司信息不足");
        verify(jobMapper, never()).insert(any(Job.class));
        verify(itemMapper).updateById(item);
    }

    @Test
    void markDuplicate_shouldLinkExistingJob() {
        JobCrawlItem item = pendingItem(1L);
        when(itemMapper.selectById(1L)).thenReturn(item);

        AdminJobCrawlItemReviewRequest request = new AdminJobCrawlItemReviewRequest();
        request.setDuplicateOfJobId(99L);
        reviewService.markDuplicate(1L, request);

        assertThat(item.getReviewStatus()).isEqualTo("duplicate");
        assertThat(item.getDuplicateOfJobId()).isEqualTo(99L);
        verify(itemMapper).updateById(item);
    }

    @Test
    void approve_shouldThrowWhenItemNotFound() {
        when(itemMapper.selectById(999L)).thenReturn(null);
        AdminJobCrawlItemReviewRequest request = new AdminJobCrawlItemReviewRequest();

        assertThatThrownBy(() -> reviewService.approve(999L, request))
                .hasMessageContaining("采集项不存在");
    }

    private JobCrawlItem pendingItem(Long id) {
        JobCrawlItem item = new JobCrawlItem();
        item.setId(id);
        item.setReviewStatus("pending_review");
        item.setSourceUrl("https://example.com/job/1");
        item.setSourcePlatform("website");
        item.setExtractedJson("{\"title\":\"Java后端\",\"companyName\":\"示例科技\",\"city\":\"杭州\",\"salaryRange\":\"20-30K\",\"experienceRequirement\":\"3-5年\",\"educationRequirement\":\"本科\",\"jobDescription\":\"负责后端开发\",\"jobRequirement\":\"熟悉Java\",\"techStack\":[\"Java\"],\"summary\":\"后端岗位\",\"tags\":[\"Java后端\"],\"qualityScore\":90,\"warnings\":[]}");
        return item;
    }
}

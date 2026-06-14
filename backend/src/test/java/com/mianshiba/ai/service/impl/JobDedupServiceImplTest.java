package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.model.entity.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDedupServiceImplTest {

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobDedupServiceImpl dedupService;

    @Test
    void normalizeUrl_shouldRemoveTrackingParams() {
        String result = dedupService.normalizeUrl("https://example.com/job?id=123&utm_source=foo");
        assertThat(result).isEqualTo("https://example.com/job?id=123");
    }

    @Test
    void normalizeUrl_shouldRemoveRefParam() {
        String result = dedupService.normalizeUrl("https://example.com/job?id=123&ref=homepage");
        assertThat(result).isEqualTo("https://example.com/job?id=123");
    }

    @Test
    void normalizeUrl_shouldRemoveSpmParam() {
        String result = dedupService.normalizeUrl("https://example.com/job?spm=abc&id=456");
        assertThat(result).isEqualTo("https://example.com/job?id=456");
    }

    @Test
    void normalizeUrl_shouldRemoveTrackParam() {
        String result = dedupService.normalizeUrl("https://example.com/job?track=campaign&id=789");
        assertThat(result).isEqualTo("https://example.com/job?id=789");
    }

    @Test
    void normalizeUrl_shouldLowercaseAndTrim() {
        String result = dedupService.normalizeUrl("  HTTPS://Example.COM/Job?ID=100  ");
        assertThat(result).isEqualTo("https://example.com/job?id=100");
    }

    @Test
    void buildFingerprint_shouldUseCompanyTitleAndCity() {
        Job job = new Job();
        job.setCompanyName("Example Inc.");
        job.setTitle("Senior Dev");
        job.setCity("Beijing");
        String fp = dedupService.buildFingerprint(job);
        assertThat(fp).isEqualTo("example inc.|senior dev|beijing");
    }

    @Test
    void buildFingerprint_shouldHandleNullFields() {
        Job job = new Job();
        job.setCompanyName(null);
        job.setTitle("Dev");
        job.setCity(null);
        String fp = dedupService.buildFingerprint(job);
        assertThat(fp).isEqualTo("|dev|");
    }

    @Test
    void findDuplicate_shouldReturnNullWhenNoMatch() {
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThat(dedupService.findDuplicate("https://example.com/job?id=1", mockJob())).isNull();
    }

    @Test
    void findDuplicate_shouldReturnJobWhenSourceUrlMatches() {
        Job existing = mockJob();
        existing.setId(42L);
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        Job result = dedupService.findDuplicate("https://example.com/job?id=1", mockJob());
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(42L);
    }

    private Job mockJob() {
        Job job = new Job();
        job.setCompanyName("Example Inc.");
        job.setTitle("Senior Dev");
        job.setCity("Beijing");
        return job;
    }
}

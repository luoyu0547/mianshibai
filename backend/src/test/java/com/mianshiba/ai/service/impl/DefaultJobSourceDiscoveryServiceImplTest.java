package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.service.JobSourceDiscoveryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJobSourceDiscoveryServiceImplTest {

    @Test
    void discover_shouldGenerateProgrammerQueriesAndCandidateUrls() {
        DefaultJobSourceDiscoveryServiceImpl service = new DefaultJobSourceDiscoveryServiceImpl();

        JobCrawlTask task = new JobCrawlTask();
        task.setKeywords("Java后端,Spring Boot");
        task.setCities("杭州,上海");
        task.setExperienceLevels("3-5年");
        task.setSourceUrl("https://example.com/careers");

        List<JobSourceDiscoveryService.CandidateUrl> urls = service.discover(task);

        assertThat(urls).isNotEmpty();
        assertThat(urls).anyMatch(url -> url.url().equals("https://example.com/careers"));
        assertThat(urls).noneMatch(url -> "public_search".equals(url.sourceType()));
        assertThat(urls).allMatch(url -> url.sourceType() != null && !url.sourceType().isBlank());
    }

    @Test
    void discover_shouldNotCreateSearchEngineUrlsWhenNoSourceUrlConfigured() {
        DefaultJobSourceDiscoveryServiceImpl service = new DefaultJobSourceDiscoveryServiceImpl();

        JobCrawlTask task = new JobCrawlTask();
        task.setKeywords("");
        task.setCities("");
        task.setExperienceLevels("");

        List<JobSourceDiscoveryService.CandidateUrl> urls = service.discover(task);

        assertThat(urls).isEmpty();
    }

    @Test
    void discover_shouldRejectConfiguredSearchEngineSourceUrl() {
        DefaultJobSourceDiscoveryServiceImpl service = new DefaultJobSourceDiscoveryServiceImpl();

        JobCrawlTask task = new JobCrawlTask();
        task.setSourceUrl("https://cn.bing.com/search?q=深圳+Java后端+招聘");
        task.setKeywords("Java后端");
        task.setCities("深圳");

        List<JobSourceDiscoveryService.CandidateUrl> urls = service.discover(task);

        assertThat(urls).isEmpty();
    }

    @Test
    void discover_shouldIgnoreKeywordOnlySearchCriteria() {
        DefaultJobSourceDiscoveryServiceImpl service = new DefaultJobSourceDiscoveryServiceImpl();

        JobCrawlTask task = new JobCrawlTask();
        task.setKeywords("Java,Python,Go");
        task.setCities("杭州,上海,北京");
        task.setExperienceLevels("应届,3-5年,5-10年");

        List<JobSourceDiscoveryService.CandidateUrl> urls = service.discover(task);

        long searchCount = urls.stream().filter(u -> "public_search".equals(u.sourceType())).count();
        assertThat(searchCount).isZero();
    }

    @Test
    void discover_shouldHandleSourceUrlOnly() {
        DefaultJobSourceDiscoveryServiceImpl service = new DefaultJobSourceDiscoveryServiceImpl();

        JobCrawlTask task = new JobCrawlTask();
        task.setSourceUrl("https://example.com/jobs");
        task.setKeywords("");
        task.setCities("");
        task.setExperienceLevels("");

        List<JobSourceDiscoveryService.CandidateUrl> urls = service.discover(task);

        assertThat(urls).anySatisfy(url -> {
            assertThat(url.url()).isEqualTo("https://example.com/jobs");
            assertThat(url.sourceType()).isEqualTo("direct");
            assertThat(url.sourcePlatform()).isEqualTo("unknown");
            assertThat(url.discoveryQuery()).isEqualTo("configured source");
        });
    }
}

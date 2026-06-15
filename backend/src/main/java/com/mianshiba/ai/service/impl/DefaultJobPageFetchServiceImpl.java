package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.service.JobCrawlService;
import com.mianshiba.ai.service.JobPageFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultJobPageFetchServiceImpl implements JobPageFetchService {

    private final JobCrawlService jobCrawlService;

    @Override
    public FetchedPage fetch(String url) {
        JobCrawlService.CrawlResult result = jobCrawlService.crawl(url);
        log.debug("Fetched page: {}, title: {}", result.finalUrl(), result.title());
        return new FetchedPage(result.url(), result.finalUrl(), result.title(),
                result.content(), result.html(), result.sourcePlatform(), false);
    }
}

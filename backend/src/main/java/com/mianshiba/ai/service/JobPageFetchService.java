package com.mianshiba.ai.service;

/**
 * Augments {@link JobCrawlService} with third-party fetching capability.
 * Wraps the core crawl logic and adds the {@code thirdPartyUsed} flag
 * to distinguish between direct HTTP fetches and external service calls.
 *
 * @see JobCrawlService
 */
public interface JobPageFetchService {
    FetchedPage fetch(String url);

    record FetchedPage(String sourceUrl, String finalUrl, String title, String content, String html,
                       String sourcePlatform, boolean thirdPartyUsed) {
    }
}

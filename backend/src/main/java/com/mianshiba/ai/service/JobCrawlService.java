package com.mianshiba.ai.service;

public interface JobCrawlService {

    CrawlResult crawl(String url);

    record CrawlResult(String url, String finalUrl, String title, String content, String html, String sourcePlatform) {
    }
}

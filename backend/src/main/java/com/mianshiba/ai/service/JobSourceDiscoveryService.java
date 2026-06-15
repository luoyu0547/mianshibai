package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobCrawlTask;

import java.util.List;

public interface JobSourceDiscoveryService {
    List<CandidateUrl> discover(JobCrawlTask task);

    record CandidateUrl(String url, String sourceType, String sourcePlatform, String discoveryQuery) {
    }
}

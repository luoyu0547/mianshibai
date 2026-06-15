package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlItemReviewRequest;

public interface JobSourcingReviewService {
    void approve(Long itemId, AdminJobCrawlItemReviewRequest request);
    void reject(Long itemId, AdminJobCrawlItemReviewRequest request);
    void markDuplicate(Long itemId, AdminJobCrawlItemReviewRequest request);
}

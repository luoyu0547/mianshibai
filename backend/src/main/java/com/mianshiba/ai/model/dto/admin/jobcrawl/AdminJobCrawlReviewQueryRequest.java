package com.mianshiba.ai.model.dto.admin.jobcrawl;

import lombok.Data;

@Data
public class AdminJobCrawlReviewQueryRequest {
    private String reviewStatus;
    private String keyword;
    private String city;
    private Long taskId;
}

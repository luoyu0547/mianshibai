package com.mianshiba.ai.model.dto.admin.jobcrawl;

import lombok.Data;

@Data
public class AdminJobCrawlTaskQueryRequest {
    private String name;
    private String sourceType;
    private String status;
    private String scheduleType;
}

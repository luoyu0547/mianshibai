package com.mianshiba.ai.model.dto.admin.jobcrawl;

import lombok.Data;

@Data
public class AdminJobCrawlTaskUpdateRequest {
    private String name;
    private String sourceType;
    private String sourceUrl;
    private String configJson;
    private String keywords;
    private String cities;
    private String experienceLevels;
    private String scheduleType;
    private String cronExpression;
    private String remark;
}

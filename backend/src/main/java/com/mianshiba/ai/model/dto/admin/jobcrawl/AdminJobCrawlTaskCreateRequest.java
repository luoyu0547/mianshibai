package com.mianshiba.ai.model.dto.admin.jobcrawl;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminJobCrawlTaskCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
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

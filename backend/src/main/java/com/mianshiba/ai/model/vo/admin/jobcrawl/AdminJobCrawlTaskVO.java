package com.mianshiba.ai.model.vo.admin.jobcrawl;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminJobCrawlTaskVO {
    private Long id;
    private String name;
    private String sourceType;
    private String sourceUrl;
    private String configJson;
    private String keywords;
    private String cities;
    private String experienceLevels;
    private String scheduleType;
    private String cronExpression;
    private String status;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Long createdBy;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer successCount;
    private Integer failedCount;
    private String latestStatus;
}

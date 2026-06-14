package com.mianshiba.ai.model.vo.admin.jobcrawl;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminJobCrawlItemVO {
    private Long id;
    private Long runId;
    private Long taskId;
    private String sourceUrl;
    private Long jobId;
    private String status;
    private String errorMessage;
    private String rawTitle;
    private String rawCompanyName;
    private LocalDateTime createTime;
}

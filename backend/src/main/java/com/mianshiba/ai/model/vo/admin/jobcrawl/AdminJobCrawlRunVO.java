package com.mianshiba.ai.model.vo.admin.jobcrawl;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminJobCrawlRunVO {
    private Long id;
    private Long taskId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer totalCount;
    private Integer successCount;
    private Integer duplicateCount;
    private Integer failedCount;
    private String errorMessage;
    private LocalDateTime createTime;
}

package com.mianshiba.ai.model.dto.application;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新投递记录请求
 */
@Data
public class ApplicationUpdateRequest {

    private Long jobId;
    private Long resumeId;
    private String companyName;
    private String jobTitle;
    private String source;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime nextEventAt;
    private String salaryRange;
    private String location;
    private String contactName;
    private String contactInfo;
    private String notes;
}
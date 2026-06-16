package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建投递记录请求
 */
@Data
public class ApplicationCreateRequest {

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 128, message = "公司名称不能超过128个字符")
    private String companyName;

    @NotBlank(message = "职位名称不能为空")
    @Size(max = 128, message = "职位名称不能超过128个字符")
    private String jobTitle;

    private Long resumeId;

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

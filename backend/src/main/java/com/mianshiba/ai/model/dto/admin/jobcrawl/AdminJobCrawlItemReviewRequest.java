package com.mianshiba.ai.model.dto.admin.jobcrawl;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminJobCrawlItemReviewRequest {
    private Long duplicateOfJobId;
    private String editedExtractedJson;
    @Size(max = 512, message = "审核备注不能超过512个字符")
    private String reviewNote;
}

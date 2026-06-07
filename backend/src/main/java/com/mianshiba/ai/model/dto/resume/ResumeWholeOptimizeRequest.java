package com.mianshiba.ai.model.dto.resume;

import lombok.Data;

@Data
public class ResumeWholeOptimizeRequest {
    private Long resumeId;
    private Long jobId;
    private String targetPosition;
    private String optimizeGoal;
}

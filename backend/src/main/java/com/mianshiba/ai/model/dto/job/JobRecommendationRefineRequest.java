package com.mianshiba.ai.model.dto.job;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobRecommendationRefineRequest {

    @NotNull
    private Long resumeId;
}

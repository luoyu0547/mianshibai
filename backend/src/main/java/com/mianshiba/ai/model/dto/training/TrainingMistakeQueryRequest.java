package com.mianshiba.ai.model.dto.training;

import lombok.Data;

@Data
public class TrainingMistakeQueryRequest {
    private String topic;
    private String masteryLevel;
    private Boolean includeMastered;
    private Integer scoreMax;
}

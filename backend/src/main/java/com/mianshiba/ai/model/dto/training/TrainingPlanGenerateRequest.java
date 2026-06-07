package com.mianshiba.ai.model.dto.training;

import lombok.Data;

/**
 * 生成训练计划请求
 */
@Data
public class TrainingPlanGenerateRequest {

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 目标天数
     */
    private Integer targetDays;

    /**
     * 目标职位
     */
    private String targetPosition;
}

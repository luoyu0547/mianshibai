package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练计划 VO
 */
@Data
public class TrainingPlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String sourceType;
    private Long sourceId;
    private Integer targetDays;
    private String status;
    private String summary;
    private List<String> focusTopics;
    private List<TrainingQuestionVO> questions;
    private List<AlgorithmRecommendationVO> algorithmRecommendations;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

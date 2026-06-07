package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练题目 VO
 */
@Data
public class TrainingQuestionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private Integer dayIndex;
    private String title;
    private String content;
    private String topic;
    private List<String> skillTags;
    private String difficulty;
    private String sourceType;
    private String referenceAnswer;
    private List<String> followUpQuestions;
    private String status;
    private Integer latestScore;
    private String latestMasteryLevel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

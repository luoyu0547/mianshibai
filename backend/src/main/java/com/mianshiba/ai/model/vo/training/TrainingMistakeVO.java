package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainingMistakeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long questionId;
    private Long planId;
    private String title;
    private String content;
    private String topic;
    private List<String> skillTags;
    private String difficulty;
    private String status;
    private Integer latestScore;
    private String masteryLevel;
    private List<String> mistakes;
    private List<String> missingPoints;
    private List<String> suggestions;
    private String recommendedAnswer;
    private LocalDateTime lastAnsweredAt;
}

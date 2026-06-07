package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练答案评审 VO
 */
@Data
public class TrainingAnswerReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long answerId;
    private Integer totalScore;
    private Integer accuracyScore;
    private Integer clarityScore;
    private Integer depthScore;
    private Integer projectScore;
    private List<String> strengths;
    private List<String> mistakes;
    private List<String> missingPoints;
    private List<String> suggestions;
    private String recommendedAnswer;
    private List<String> followUpQuestions;
    private String masteryLevel;
    private LocalDateTime createTime;
}

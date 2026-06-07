package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 训练答案 VO
 */
@Data
public class TrainingAnswerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long questionId;
    private String answerText;
    private TrainingAnswerReviewVO review;
    private LocalDateTime createTime;
}

package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InterviewSessionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long resumeId;

    private String title;

    private String interviewType;

    private String targetPosition;

    private String techDirection;

    private Integer totalQuestions;

    private Integer currentQuestionNo;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String difficulty;

    private Integer durationMinutes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

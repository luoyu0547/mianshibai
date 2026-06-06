package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InterviewTurnVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sessionId;

    private Integer questionNo;

    private String turnType;

    private String questionText;

    private String answerText;

    private String aiFeedback;

    private Integer answerDurationSeconds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

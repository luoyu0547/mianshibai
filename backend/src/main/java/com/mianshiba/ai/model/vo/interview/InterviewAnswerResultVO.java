package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class InterviewAnswerResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nextAction;

    private InterviewQuestionVO turn;

    private Long reportId;
}

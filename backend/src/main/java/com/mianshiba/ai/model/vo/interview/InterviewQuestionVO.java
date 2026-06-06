package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class InterviewQuestionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long turnId;

    private Integer questionNo;

    private String turnType;

    private String questionText;

    private String ttsAudioBase64;
}

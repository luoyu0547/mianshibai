package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class InterviewTurnReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long turnId;
    private String question;
    private String answerSummary;
    private String diagnosis;
    private String excellentAnswer;
    private String improvedAnswer;
    private List<String> knowledgePoints;
}

package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class JobQuestionPredictionVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<String> technicalQuestions;
    private List<String> projectQuestions;
    private List<String> systemDesignQuestions;
    private List<String> hrQuestions;
}

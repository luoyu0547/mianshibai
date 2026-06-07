package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class InterviewScoreDeltaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String key;
    private String label;
    private Integer baseScore;
    private Integer targetScore;
    private Integer delta;
}

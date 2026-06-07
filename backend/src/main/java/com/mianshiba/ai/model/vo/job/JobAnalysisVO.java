package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 职位分析 VO
 */
@Data
public class JobAnalysisVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String requirementSummary;

    private String coreSkills;

    private String hiddenRequirements;

    private String interviewFocus;

    private String resumeSuggestions;

    private String riskPoints;
}

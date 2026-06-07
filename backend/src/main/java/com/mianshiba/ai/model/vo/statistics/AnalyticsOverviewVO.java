package com.mianshiba.ai.model.vo.statistics;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AnalyticsOverviewVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long resumeCount;
    private Long jobCount;
    private Long interviewCount;
    private Integer averageInterviewScore;
    private List<String> topMissingSkills;
    private List<String> nextActions;
}

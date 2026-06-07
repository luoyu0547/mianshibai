package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class InterviewReportCompareVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long baseSessionId;
    private Long targetSessionId;
    private Integer baseTotalScore;
    private Integer targetTotalScore;
    private Integer totalDelta;
    private List<InterviewScoreDeltaVO> dimensions;
    private List<String> newSkillGaps;
    private List<String> resolvedSkillGaps;
    private List<String> summary;
}

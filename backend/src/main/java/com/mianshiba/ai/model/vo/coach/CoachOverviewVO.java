package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 求职教练总览 VO
 */
@Data
public class CoachOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private CoachDiagnosisVO latestDiagnosis;
    private CoachPlanVO activePlan;
    private List<CoachTaskVO> todayTasks;
    private Long diagnosisCount;
    private Long planCount;
}

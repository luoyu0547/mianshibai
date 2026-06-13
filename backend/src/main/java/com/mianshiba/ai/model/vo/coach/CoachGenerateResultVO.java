package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 求职教练生成结果 VO
 */
@Data
public class CoachGenerateResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private CoachDiagnosisVO diagnosis;
    private CoachPlanVO plan;
}

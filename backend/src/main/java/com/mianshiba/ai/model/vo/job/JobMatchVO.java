package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 职位匹配 VO
 */
@Data
public class JobMatchVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Integer matchScore;

    private Integer growthScore;

    private Integer techGrowthScore;

    private Integer salaryCityScore;

    private Integer experienceFitScore;

    private Integer totalScore;

    private String recommendation;

    private String reason;

    private String gaps;
}

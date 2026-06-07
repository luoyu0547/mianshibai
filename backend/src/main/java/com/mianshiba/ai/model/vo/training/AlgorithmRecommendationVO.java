package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 算法推荐 VO
 */
@Data
public class AlgorithmRecommendationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private String category;
    private String platform;
    private String problemRef;
    private String reason;
    private Integer completed;
}

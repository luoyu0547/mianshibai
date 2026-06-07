package com.mianshiba.ai.model.vo.application;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 投递统计 VO
 */
@Data
public class ApplicationStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long total;
    private Long pendingSubmit;
    private Long submitted;
    private Long interviewing;
    private Long offer;
    private Long closed;
}
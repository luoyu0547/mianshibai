package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TrainingMasterySummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long weak;
    private Long basic;
    private Long good;
    private Long mastered;
}

package com.mianshiba.ai.model.vo.statistics;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class HomeStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long completedInterviews;
    private long totalQuestions;
    private long practiceDays;
}

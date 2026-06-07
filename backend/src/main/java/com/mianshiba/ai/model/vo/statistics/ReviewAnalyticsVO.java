package com.mianshiba.ai.model.vo.statistics;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ReviewAnalyticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Map<String, Integer> radar;
    private List<Map<String, String>> topSkillGaps;
    private List<Map<String, Object>> recentScoreTrend;
    private List<String> latestActionItems;
}

package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class InterviewReportEnhancementVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Long reportId;
    private String status;
    private String summary;
    private Map<String, Integer> radar;
    private List<Map<String, String>> skillGaps;
    private List<String> actionItems;
    private String errorMessage;
    private Integer retryCount;
    private List<InterviewTurnReviewVO> turnReviews;
}

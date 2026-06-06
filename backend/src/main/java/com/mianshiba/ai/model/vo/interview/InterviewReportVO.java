package com.mianshiba.ai.model.vo.interview;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sessionId;

    private Integer totalScore;

    private Integer accuracyScore;

    private Integer clarityScore;

    private Integer depthScore;

    private Integer matchingScore;

    private String summary;

    private List<String> suggestions;

    private List<InterviewTurnVO> turns;

    private LocalDateTime createTime;
}

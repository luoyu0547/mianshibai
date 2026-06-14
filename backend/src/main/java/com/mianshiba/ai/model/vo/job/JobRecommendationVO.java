package com.mianshiba.ai.model.vo.job;

import com.mianshiba.ai.model.vo.application.JobApplicationVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class JobRecommendationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private JobVO job;
    private String stage;
    private Integer roughScore;
    private JobMatchVO matchResult;
    private String recommendation;
    private String reason;
    private List<String> riskPoints;
    private List<String> actionSuggestions;
    private Boolean dismissed;
    private Boolean applied;
}

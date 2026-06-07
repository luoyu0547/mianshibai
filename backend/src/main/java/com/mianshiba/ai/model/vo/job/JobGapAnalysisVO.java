package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class JobGapAnalysisVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Integer keywordCoverage;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private List<String> projectExpressionGaps;
    private List<String> optimizeActions;
}

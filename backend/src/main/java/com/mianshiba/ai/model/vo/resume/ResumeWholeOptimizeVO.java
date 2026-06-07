package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ResumeWholeOptimizeVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Integer beforeScore;
    private Integer estimatedAfterScore;
    private List<String> globalSuggestions;
    private List<SectionVO> optimizedSections;
}

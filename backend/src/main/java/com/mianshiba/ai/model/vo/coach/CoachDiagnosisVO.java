package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CoachDiagnosisVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private Integer overallScore;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
    private Map<String, Object> dataSnapshot;
    private Integer dataCompleteness;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

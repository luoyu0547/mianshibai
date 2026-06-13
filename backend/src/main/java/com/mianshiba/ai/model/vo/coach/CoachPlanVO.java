package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CoachPlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long diagnosisId;
    private String title;
    private String summary;
    private String targetPosition;
    private Integer targetDays;
    private String status;
    private String source;
    private Integer totalTaskCount;
    private Integer completedTaskCount;
    private List<CoachTaskVO> tasks;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CoachTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private Integer dayIndex;
    private String title;
    private String description;
    private String taskType;
    private String priority;
    private String status;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

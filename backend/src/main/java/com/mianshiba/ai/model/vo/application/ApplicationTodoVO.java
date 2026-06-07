package com.mianshiba.ai.model.vo.application;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 投递待办 VO
 */
@Data
public class ApplicationTodoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long applicationId;
    private String applicationCompanyName;
    private String applicationJobTitle;
    private String title;
    private String description;
    private String priority;
    private String priorityLabel;
    private LocalDateTime dueAt;
    private Boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
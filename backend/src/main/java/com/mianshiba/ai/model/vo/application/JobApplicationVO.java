package com.mianshiba.ai.model.vo.application;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 投递记录 VO
 */
@Data
public class JobApplicationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long jobId;
    private Long resumeId;
    private String companyName;
    private String jobTitle;
    private String source;
    private String status;
    private String statusLabel;
    private LocalDateTime appliedAt;
    private LocalDateTime nextEventAt;
    private String salaryRange;
    private String location;
    private String contactName;
    private String contactInfo;
    private String notes;
    private Integer unfinishedTodoCount;
    private List<ApplicationTodoVO> todos;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
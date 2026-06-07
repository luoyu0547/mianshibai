package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位分析实体
 */
@Data
@TableName("job_analysis")
public class JobAnalysis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private String requirementSummary;

    private String coreSkills;

    private String hiddenRequirements;

    private String interviewFocus;

    private String resumeSuggestions;

    private String riskPoints;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

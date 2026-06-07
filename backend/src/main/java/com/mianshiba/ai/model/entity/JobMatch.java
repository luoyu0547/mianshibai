package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位匹配实体
 */
@Data
@TableName("job_match")
public class JobMatch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jobId;

    private Integer matchScore;

    private Integer growthScore;

    private Integer techGrowthScore;

    private Integer salaryCityScore;

    private Integer experienceFitScore;

    private Integer totalScore;

    private String recommendation;

    private String reason;

    private String gaps;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

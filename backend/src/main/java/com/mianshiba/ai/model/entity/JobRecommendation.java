package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("job_recommendation")
public class JobRecommendation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jobId;

    private String stage;

    private Integer roughScore;

    private Long matchId;

    private String recommendation;

    private String reason;

    private Object riskPointsJson;

    private Object actionSuggestionsJson;

    private String source;

    private Integer dismissed;

    private Integer applied;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

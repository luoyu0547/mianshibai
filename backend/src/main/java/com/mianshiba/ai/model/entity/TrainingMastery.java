package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("training_mastery")
public class TrainingMastery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType;
    private String targetName;
    private Integer practiceCount;
    private Integer questionCount;
    private BigDecimal averageScore;
    private Integer weakCount;
    private Integer masteredCount;
    private String masteryLevel;
    private LocalDateTime lastPracticedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDelete;
}

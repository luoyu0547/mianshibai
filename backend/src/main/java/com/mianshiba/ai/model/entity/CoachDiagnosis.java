package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 求职教练诊断报告实体
 */
@Data
@TableName(value = "coach_diagnosis", autoResultMap = true)
public class CoachDiagnosis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private Integer overallScore;
    private String summary;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> strengthsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> weaknessesJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> suggestionsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dataSnapshotJson;

    private Integer dataCompleteness;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

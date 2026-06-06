package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "interview_report", autoResultMap = true)
public class InterviewReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Integer totalScore;

    private Integer accuracyScore;

    private Integer clarityScore;

    private Integer depthScore;

    private Integer matchingScore;

    private String summary;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> suggestions;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

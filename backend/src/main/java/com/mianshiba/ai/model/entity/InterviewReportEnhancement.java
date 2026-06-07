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

@Data
@TableName(value = "interview_report_enhancement", autoResultMap = true)
public class InterviewReportEnhancement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long reportId;
    private String status;
    private String summary;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Integer> radarJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> skillGapsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> actionItemsJson;

    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

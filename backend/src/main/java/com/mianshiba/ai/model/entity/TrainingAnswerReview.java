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

@Data
@TableName(value = "training_answer_review", autoResultMap = true)
public class TrainingAnswerReview implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private Long answerId;
    private Integer totalScore;
    private Integer accuracyScore;
    private Integer clarityScore;
    private Integer depthScore;
    private Integer projectScore;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> strengthsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> mistakesJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> missingPointsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> suggestionsJson;

    private String recommendedAnswer;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> followUpQuestionsJson;

    private String masteryLevel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

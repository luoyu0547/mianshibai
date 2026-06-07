package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位实体
 */
@Data
@TableName("job")
public class Job implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long companyId;

    private String title;

    private String companyName;

    private String sourcePlatform;

    private String sourceUrl;

    private String city;

    private String salaryRange;

    private String experienceRequirement;

    private String educationRequirement;

    private String jobDescription;

    private String jobRequirement;

    private String techStack;

    private String rawContent;

    private String status;

    private String applicationStatus;

    private String keywordsJson;

    private String predictedQuestionsJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

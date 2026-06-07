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
 * 求职投递记录实体
 */
@Data
@TableName("job_application")
public class JobApplication implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long jobId;
    private Long resumeId;
    private String companyName;
    private String jobTitle;
    private String source;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime nextEventAt;
    private String salaryRange;
    private String location;
    private String contactName;
    private String contactInfo;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
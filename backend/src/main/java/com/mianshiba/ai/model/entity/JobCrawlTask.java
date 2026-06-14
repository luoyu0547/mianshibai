package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("job_crawl_task")
public class JobCrawlTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String sourceType;

    private String sourceUrl;

    private Object configJson;

    private String keywords;

    private String cities;

    private String experienceLevels;

    private String scheduleType;

    private String cronExpression;

    private String status;

    private LocalDateTime lastRunAt;

    private LocalDateTime nextRunAt;

    private Long createdBy;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

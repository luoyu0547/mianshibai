package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("job_crawl_run")
public class JobCrawlRun implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Integer totalCount;

    private Integer successCount;

    private Integer duplicateCount;

    private Integer failedCount;

    private String errorMessage;

    private LocalDateTime createTime;
}

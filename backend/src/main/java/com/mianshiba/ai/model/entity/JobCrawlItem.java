package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("job_crawl_item")
public class JobCrawlItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private Long taskId;

    private String sourceUrl;

    private String normalizedUrl;

    private Long jobId;

    private String status;

    private String errorMessage;

    private String rawTitle;

    private String rawCompanyName;

    private LocalDateTime createTime;
}

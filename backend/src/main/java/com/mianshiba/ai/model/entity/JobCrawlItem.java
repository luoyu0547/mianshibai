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

    /**
     * 来源平台标识
     */
    private String sourcePlatform;

    /**
     * 页面原始内容
     */
    private String rawContent;

    /**
     * 内容摘要
     */
    private String summary;

    /**
     * 质量评分
     */
    private Integer qualityScore;

    /**
     * 提取置信度
     */
    private Integer confidenceScore;

    /**
     * 提取后的结构化 JSON
     */
    private String extractedJson;

    /**
     * 标签 JSON
     */
    private String tagsJson;

    /**
     * 人工复核状态
     */
    private String reviewStatus;

    /**
     * 人工复核备注
     */
    private String reviewNote;

    /**
     * 疑似重复的正式职位 id
     */
    private Long duplicateOfJobId;

    private LocalDateTime createTime;
}

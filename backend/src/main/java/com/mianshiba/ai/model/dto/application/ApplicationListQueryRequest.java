package com.mianshiba.ai.model.dto.application;

import lombok.Data;

/**
 * 投递记录列表查询请求
 */
@Data
public class ApplicationListQueryRequest {

    private String keyword;
    private String status;
    private String location;
    private String source;
    private Long jobId;
    private Long resumeId;
}
package com.mianshiba.ai.model.dto.application;

import lombok.Data;

/**
 * 投递待办查询请求
 */
@Data
public class ApplicationTodoQueryRequest {

    private Long applicationId;
    private Boolean completed;
    private String priority;
    private Boolean overdue;
}
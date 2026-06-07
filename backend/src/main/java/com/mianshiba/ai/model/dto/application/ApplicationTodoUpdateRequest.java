package com.mianshiba.ai.model.dto.application;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新投递待办请求
 */
@Data
public class ApplicationTodoUpdateRequest {

    private String title;
    private String description;
    private String priority;
    private LocalDateTime dueAt;
}
package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建投递待办请求
 */
@Data
public class ApplicationTodoCreateRequest {

    @NotBlank(message = "待办标题不能为空")
    @Size(max = 128, message = "待办标题不能超过128个字符")
    private String title;

    private String description;

    private String priority;

    private LocalDateTime dueAt;
}
package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新投递状态请求
 */
@Data
public class ApplicationStatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
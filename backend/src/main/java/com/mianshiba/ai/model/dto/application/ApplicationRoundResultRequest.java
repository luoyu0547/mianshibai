package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationRoundResultRequest {
    @NotBlank(message = "结果不能为空")
    private String result;
}
